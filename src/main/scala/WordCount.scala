
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
import scala.collection.JavaConverters.*

class WordCount
object WordCount {

  val config: Config = ConfigFactory.load("application.conf")
  val taskConfig = config.getConfig("randomLogGenerator.taskConfigs.wordCount")

  val logger = CreateLogger(classOf[WordCount])
  logger.info(s"Test config loading, minimum string size is: ${config.getString("randomLogGenerator.MinString")}")


  class TokenizerMapper extends Mapper[Object, Text, Text, IntWritable] {

    val one = new IntWritable(1)
    val word = new Text()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, IntWritable]#Context): Unit = {

      // Load the regex to detect whatever type instance we need to find from config and create a matcher for it. This matcher is applied to the log message
      // read at runtime and isolates matches. The group() method returns the matched substring for the desired type instance.

      val keywordPatternMatcher = Pattern.compile(taskConfig.getString("detectTypeInstancesOf")).matcher(value.toString)
      if(keywordPatternMatcher.find()) {
        logger.info(s"Match found for one of- ${taskConfig.getString("detectTypeInstancesOf")}")
        word.set(keywordPatternMatcher.group())
        context.write(word, one)
      }
    }
  }

  class IntSumReader extends Reducer[Text,IntWritable,Text,IntWritable] {
    override def reduce(key: Text, values: Iterable[IntWritable], context: Reducer[Text, IntWritable, Text, IntWritable]#Context): Unit = {

      // The reducer implements a simple running count by using the foldLeft method on the values iterable to count number of instances for the key,
      // where the key is a detected type instance.

      val sum = values.asScala.foldLeft(0)(_ + _.get)
      logger.info(s"Sum calculated is ${sum}")
      context.write(key, new IntWritable(sum))
    }
  }


  def run(args: Array[String]): Unit = {
    val configuration = new Configuration

    // Creating a CSV
    configuration.set("mapred.textoutputformat.separator", ",")

    val job = Job.getInstance(configuration, taskConfig.getString("jobName"))
    job.setJarByClass(this.getClass)
    job.setMapperClass(classOf[TokenizerMapper])
    job.setCombinerClass(classOf[IntSumReader])
    job.setReducerClass(classOf[IntSumReader])
    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[IntWritable])

    // Specifying input and output for the program, this is received from the cli and the actual folder name is specified in the config
    FileInputFormat.addInputPath(job, new Path(args(0)))
    FileOutputFormat.setOutputPath(job, new Path(args(1) + taskConfig.getString("outputFileName")))

    job.waitForCompletion(true)
  }

}