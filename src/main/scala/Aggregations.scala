
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
  //  val logger = CreateLogger(classOf[WordCount])
  //  logger.info(s"Test config loading, minimum string size is: ${config.getString("randomLogGenerator.MinString")}")


  class TokenizerMapper extends Mapper[Object, Text, Text, Text] {

    val one = new IntWritable(1)
    val word = new Text()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, Text]#Context): Unit = {
      val itr = new StringTokenizer(value.toString)
      //      val current = value.toStringSS
      //      current.split("\\$ - ")
      //      val current = value.toString.split("\\$ - ")(1)
      val injectedStringPatternMatcher = Pattern.compile(config.getString("randomLogGenerator.Pattern")).matcher(value.toString)
      val keywordPatternMatcher = Pattern.compile("(DEBUG)|(INFO)|(WARN)|(ERROR)").matcher(value.toString)
      if(injectedStringPatternMatcher.find() && keywordPatternMatcher.find()) {
        val nextMinuteBin = String.format("%02d", value.toString.substring(3, 5).toInt + 1)
        val hourBin = value.toString.substring(0, 3)
        if(nextMinuteBin == "60" && hourBin == "24") {
          word.set(value.toString.substring(0, 5) + "-" + "00:00" + " " + keywordPatternMatcher.group())
        }
        else if(nextMinuteBin == "60") {
          word.set(value.toString.substring(0, 5) + "-" + String.format("%02d", value.toString.substring(0, 2).toInt + 1) + ":00" + " " + keywordPatternMatcher.group())
        }
        else
          word.set(value.toString.substring(0, 5) + "-" + value.toString.substring(0, 3) + String.format("%02d", value.toString.substring(3, 5).toInt + 1) + " " + keywordPatternMatcher.group())
//        context.write(word, new Text(keywordPatternMatcher.group() + ", " + injectedStringPatternMatcher.group()))
        context.write(word, new Text(injectedStringPatternMatcher.group()))
      }
    }
  }

  class extraMapper extends Mapper[Object, Text, Text, Text] {

    val word = new Text()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, Text]#Context): Unit = {
      val splitTokens = value.toString.split("===>")
      val seperateKey = splitTokens(0).split("\t")
      splitTokens.update(0, seperateKey(1))
      word.set(splitTokens.size.toString + " => " + splitTokens.mkString("===>"))
      context.write(new Text(seperateKey(0)), word)
    }
  }

  class IntSumReader extends Reducer[Text,Text,Text,Text] {
    override def reduce(key: Text, values: lang.Iterable[Text], context: Reducer[Text, Text, Text, Text]#Context): Unit = {
//      val infoCount = values.asScala.toList.count(x => {x.toString.contains("INFO")})
//      val errorCount = values.asScala.toList.count(x => {x.toString.contains("ERROR")})
//      val debugCount = values.asScala.toList.count(x => {x.toString.contains("DEBUG")})
//      val warnCount = values.asScala.toList.count(x => {x.toString.contains("WARN")})
//      values.asScala.foreach(e => {
//        e match {
//          case
//        }
//      })
//      values.asScala.groupBy(identity).mapValues(_.toString.contains("ERROR")
//      values.foldLeft(0)(_.toString.contains("INFO"))

//      val infoStrings = values.asScala.filter(x => {x.toString.contains("INFO")})
//      val errorStrings = values.asScala.filter(x => {x.toString.contains("ERROR")})
//      val debugStrings = values.asScala.filter(x => {x.toString.contains("DEBUG")})
//      val warnStrings = values.asScala.filter(x => {x.toString.contains("WARN")})
//        values

//      val infoStrings = values.asScala.filter(x => {x.toString.contains("INFO")}).foreach(e => {e.toString.split(", ")(1)})
//      val errorStrings = values.asScala.filter(x => {x.toString.contains("ERROR")}).foreach(e => {e.toString.split(", ")(1)})
//      val debugStrings = values.asScala.filter(x => {x.toString.contains("DEBUG")}).foreach(e => {e.toString.split(", ")(1)})
//      val warnStrings = values.asScala.filter(x => {x.toString.contains("WARN")}).foreach(e => {e.toString.split(", ")(1)})
//      context.write(key, new Text(values.asScala.toString))
      context.write(key, new Text(values.asScala.mkString("===>")))
      //      var sum = values.asScala.foldLeft(0)(_ + _.get)
      //      val temp = new IntWritable(values.asScala.max)
//      context.write(key, new Text(infoCount.toString))
//      context.write(key, new Text(errorCount.toString))
//      context.write(key, new Text(debugCount.toString))
//      context.write(key, new Text(warnCount.toString))
//      context.write(key, new Text(infoStrings.mkString(" => ")))
//      context.write(key, new Text(errorStrings.mkString(" => ")))
//      context.write(key, new Text(debugStrings.toString()))
//      context.write(key, new Text(warnStrings.toString()))
    }
  }

  class extraReducer extends Reducer[Text,Text,Text,Text] {

    override def reduce(key: Text, values: lang.Iterable[Text], context: Reducer[Text, Text, Text, Text]#Context): Unit = super.reduce(key, values, context)

  }


  def main(args: Array[String]): Unit = {
    val configuration = new Configuration
    val job = Job.getInstance(configuration,"Aggregate in time bins")
    job.setJarByClass(this.getClass)
    job.setMapperClass(classOf[TokenizerMapper])
    job.setCombinerClass(classOf[IntSumReader])
    job.setReducerClass(classOf[IntSumReader])
    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[Text]);
    FileInputFormat.addInputPath(job, new Path(args(0)))
    FileOutputFormat.setOutputPath(job, new Path(args(1)))
//    System.exit(if(job.waitForCompletion(true))  0 else 1)

    job.waitForCompletion(true)

    val configuration2 = new Configuration
    val job2 = Job.getInstance(configuration,"Aggregate in time bins")
    job2.setJarByClass(this.getClass)
    job2.setMapperClass(classOf[extraMapper])
    //    job2.setCombinerClass(classOf[extraReducer])
    job2.setReducerClass(classOf[extraReducer])
    //    job.setCombinerClass(classOf[IntSumReader])
    //    job.setReducerClass(classOf[IntSumReader])
    job2.setOutputKeyClass(classOf[Text])
    job2.setOutputValueClass(classOf[Text])
    FileInputFormat.addInputPath(job2, new Path(args(1)))
    FileOutputFormat.setOutputPath(job2, new Path("output3"))
    System.exit(if(job2.waitForCompletion(true))  0 else 1)
  }

}