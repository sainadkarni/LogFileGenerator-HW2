
import HelperUtils.CreateLogger
import com.typesafe.config.{Config, ConfigFactory}

import java.lang.Iterable
import java.util.StringTokenizer
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import org.apache.hadoop.mapreduce.{Job, Mapper, Reducer}

import java.util.regex.Pattern
import java.{lang, util}
import scala.collection.JavaConverters.*

class Aggregations
object Aggregations {

  val config: Config = ConfigFactory.load("application.conf")
  val taskConfig = config.getConfig("randomLogGenerator.taskConfigs.aggregate")

  val logger = CreateLogger(classOf[Aggregations])
  logger.info(s"Test config loading, minimum string size is: ${config.getString("randomLogGenerator.MinString")}")


  class TokenizerMapper extends Mapper[Object, Text, Text, Text] {

    val word = new Text()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, Text]#Context): Unit = {

      val injectedStringPatternMatcher = Pattern.compile(config.getString("randomLogGenerator.Pattern")).matcher(value.toString)
      val keywordPatternMatcher = Pattern.compile(taskConfig.getString("detectTypeInstancesOf")).matcher(value.toString)
      if(injectedStringPatternMatcher.find() && keywordPatternMatcher.find()) {
        val nextMinuteBin = String.format("%02d", value.toString.substring(3, 5).toInt + 1)
        val hourBin = value.toString.substring(0, 3)
        if(nextMinuteBin == "60" && hourBin == "23") {
          word.set(value.toString.substring(0, 5) + "-" + "00:00" + " " + keywordPatternMatcher.group())
        }
        else if(nextMinuteBin == "60") {
          word.set(value.toString.substring(0, 5) + "-" + String.format("%02d", value.toString.substring(0, 2).toInt + 1) + ":00" + " " + keywordPatternMatcher.group())
        }
        else
          word.set(value.toString.substring(0, 5) + "-" + value.toString.substring(0, 3) + String.format("%02d", value.toString.substring(3, 5).toInt + 1) + " " + keywordPatternMatcher.group())
        context.write(word, new Text(injectedStringPatternMatcher.group()))
      }
    }
  }

  class extraMapper extends Mapper[Object, Text, Text, Text] {

    val word = new Text()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, Text]#Context): Unit = {
      val splitTokens = value.toString.split(taskConfig.getString("stringInstanceSeparator"))
      val seperateKey = splitTokens(0).split("\t")
      splitTokens.update(0, seperateKey(1))
      word.set(splitTokens.size.toString + " => " + splitTokens.mkString(taskConfig.getString("stringInstanceSeparator")))
      context.write(new Text(seperateKey(0)), word)
    }
  }

  class IntSumReader extends Reducer[Text,Text,Text,Text] {
    override def reduce(key: Text, values: lang.Iterable[Text], context: Reducer[Text, Text, Text, Text]#Context): Unit = {
      context.write(key, new Text(values.asScala.mkString(taskConfig.getString("stringInstanceSeparator"))))
    }
  }

  class extraReducer extends Reducer[Text,Text,Text,Text] {

    override def reduce(key: Text, values: lang.Iterable[Text], context: Reducer[Text, Text, Text, Text]#Context): Unit = super.reduce(key, values, context)
  }


  def run(args: Array[String]): Unit = {
    val configuration = new Configuration
    val job = Job.getInstance(configuration, taskConfig.getString("jobName"))
    job.setJarByClass(this.getClass)
    job.setMapperClass(classOf[TokenizerMapper])
    job.setCombinerClass(classOf[IntSumReader])
    job.setReducerClass(classOf[IntSumReader])
    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[Text])
    FileInputFormat.addInputPath(job, new Path(args(0)))
    FileOutputFormat.setOutputPath(job, new Path(args(1) + "intermediaryTask1"))
    job.waitForCompletion(true)

    val configuration2 = new Configuration
    configuration2.set("mapred.textoutputformat.separator", ",")
    val job2 = Job.getInstance(configuration2, taskConfig.getString("jobName"))
    job2.setJarByClass(this.getClass)
    job2.setMapperClass(classOf[extraMapper])
    job2.setReducerClass(classOf[extraReducer])
    job2.setOutputKeyClass(classOf[Text])
    job2.setOutputValueClass(classOf[Text])
    FileInputFormat.addInputPath(job2, new Path(args(1) + "intermediaryTask1"))
    FileOutputFormat.setOutputPath(job2, new Path(args(1) + taskConfig.getString("outputFileName")))
    System.exit(if(job2.waitForCompletion(true))  0 else 1)
  }

}