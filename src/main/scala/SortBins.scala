
import HelperUtils.CreateLogger
import com.typesafe.config.{Config, ConfigFactory}

import java.lang.Iterable
import java.util.StringTokenizer
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, LongWritable, Text, WritableComparator}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import org.apache.hadoop.mapreduce.{Job, Mapper, Reducer}

import java.nio.ByteBuffer
import java.{lang, util}
import java.util.regex.Pattern
import scala.collection.JavaConverters.*

class SortBins
object SortBins {

  val config: Config = ConfigFactory.load("application.conf")
  val taskConfig = config.getConfig("randomLogGenerator.taskConfigs.sortBins")

  val logger = CreateLogger(classOf[SortBins])
  logger.info(s"Test config loading, minimum string size is: ${config.getString("randomLogGenerator.MinString")}")


  class TokenizerMapper extends Mapper[Object, Text, Text, IntWritable] {

    val one = new IntWritable(1)
    val word = new Text()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, IntWritable]#Context): Unit = {

      val injectedStringPatternMatcher = Pattern.compile(config.getString("randomLogGenerator.Pattern")).matcher(value.toString)
      if(injectedStringPatternMatcher.find() && value.toString.contains(taskConfig.getString("detectTypeInstancesOf"))) {
        val nextMinuteBin = String.format("%02d", value.toString.substring(3, 5).toInt + 1)
        val hourBin = value.toString.substring(0, 3)
        if(nextMinuteBin == "60" && hourBin == "24") {
          word.set(value.toString.substring(0, 5) + "-" + "00:00")
        }
        else if(nextMinuteBin == "60") {
          word.set(value.toString.substring(0, 5) + "-" + String.format("%02d", value.toString.substring(0, 2).toInt + 1) + ":00")
        }
        else
          word.set(value.toString.substring(0, 5) + "-" + value.toString.substring(0, 3) + String.format("%02d", value.toString.substring(3, 5).toInt + 1))
        context.write(word, one)
      }
    }
  }

  class extraMapper extends Mapper[Object, Text, IntWritable, Text] {

    val oneExtra = new IntWritable(1)
    val wordExtra = new Text()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, IntWritable, Text]#Context): Unit = {
      oneExtra.set(value.toString.substring(12).toInt)
      wordExtra.set(value.toString.substring(0, 11))
      context.write(oneExtra, wordExtra)
    }
  }

  class IntComparator extends WritableComparator {
    override def compare(b1: Array[Byte], s1: Int, l1: Int, b2: Array[Byte], s2: Int, l2: Int): Int = {
      val v1: Int = ByteBuffer.wrap(b1, s1, l1).getInt()
      val v2: Int = ByteBuffer.wrap(b2, s2, l2).getInt()

      v1.compareTo(v2) * (-1)
    }
  }

  class IntSumReader extends Reducer[Text,IntWritable,Text,IntWritable] {

    override def reduce(key: Text, values: Iterable[IntWritable], context: Reducer[Text, IntWritable, Text, IntWritable]#Context): Unit = {
      val sum = values.asScala.foldLeft(0)(_ + _.get)
      context.write(key, new IntWritable(sum))
    }
  }

  class extraReducer extends Reducer[IntWritable,Text,IntWritable,Text] {

    override def reduce(key: IntWritable, values: lang.Iterable[Text], context: Reducer[IntWritable, Text, IntWritable, Text]#Context): Unit = super.reduce(key, values, context)

  }



  def run(args: Array[String]): Unit = {
    val configuration = new Configuration
    val job = Job.getInstance(configuration, taskConfig.getString("jobName"))
    job.setJarByClass(this.getClass)
    job.setMapperClass(classOf[TokenizerMapper])
    job.setCombinerClass(classOf[IntSumReader])
    job.setReducerClass(classOf[IntSumReader])
    job.setOutputKeyClass(classOf[Text])
//    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[IntWritable])
//    job.setSortComparatorClass(classOf[LongWritable.DecreasingComparator])
    FileInputFormat.addInputPath(job, new Path(args(0)))
    FileOutputFormat.setOutputPath(job, new Path(args(1) + "intermediaryTask2"))
    job.waitForCompletion(true)

    val configuration2 = new Configuration
    configuration2.set("mapred.textoutputformat.separator", ",")
//    configuration2.red
    val job2 = Job.getInstance(configuration2, taskConfig.getString("jobName"))
    job2.setNumReduceTasks(1)
    job2.setJarByClass(this.getClass)
    job2.setMapperClass(classOf[extraMapper])
//    job2.setCombinerClass(classOf[extraReducer])
    job2.setReducerClass(classOf[extraReducer])
//    job.setCombinerClass(classOf[IntSumReader])
//    job.setReducerClass(classOf[IntSumReader])
    job2.setOutputKeyClass(classOf[IntWritable])
    job2.setOutputValueClass(classOf[Text])
    job2.setSortComparatorClass(classOf[IntComparator])
    FileInputFormat.addInputPath(job2, new Path(args(1) + "intermediaryTask2"))
    FileOutputFormat.setOutputPath(job2, new Path(args(1) + taskConfig.getString("outputFileName")))
    job2.waitForCompletion(true)
  }

}