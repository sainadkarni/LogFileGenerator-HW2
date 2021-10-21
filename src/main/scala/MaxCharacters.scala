
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

class MaxCharacters
object MaxCharacters {

  val config: Config = ConfigFactory.load("application.conf")
  val taskConfig = config.getConfig("randomLogGenerator.taskConfigs.maxCharacters")

  val logger = CreateLogger(classOf[MaxCharacters])
  logger.info(s"Test config loading, minimum string size is: ${config.getString("randomLogGenerator.MinString")}")


  class TokenizerMapper extends Mapper[Object, Text, Text, IntWritable] {

    val word = new Text()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, IntWritable]#Context): Unit = {

      // Matching the value log string with 2 regex expressions to determine if it fits the bill. The first is for detecting injected string instances
      // while the other is to determine the type of message printed. The value in the (k, v) pair is set to the string length, to be appropriately
      // summarized later in the reducer stage.

      val injectedStringPatternMatcher = Pattern.compile(config.getString("randomLogGenerator.Pattern")).matcher(value.toString)
      val keywordPatternMatcher = Pattern.compile(taskConfig.getString("detectTypeInstancesOf")).matcher(value.toString)
      if(injectedStringPatternMatcher.find() && keywordPatternMatcher.find()) {
        logger.info(s"Match found for one of- ${taskConfig.getString("detectTypeInstancesOf")}")
        word.set(keywordPatternMatcher.group())
        context.write(word, new IntWritable(injectedStringPatternMatcher.group().length))
      }
    }
  }

  class IntSumReader extends Reducer[Text,IntWritable,Text,IntWritable] {

    // The reducer takes the iterable value of lengths and produces the max value for a given key, which will be one of the types designated in the config
    override def reduce(key: Text, values: Iterable[IntWritable], context: Reducer[Text, IntWritable, Text, IntWritable]#Context): Unit = {
      context.write(key, values.asScala.max)
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