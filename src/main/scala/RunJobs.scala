
import HelperUtils.{CreateLogger, ObtainConfigReference}
import com.typesafe.config.ConfigFactory
//import org.slf4j.LoggerFactory

class RunJobs
object RunJobs:
  val logger = CreateLogger(classOf[RunJobs])

  def main(args: Array[String]): Unit = {
    logger.info("Starting Map/Reduce jobs")
    WordCount.run(args)
    MaxCharacters.run(args)
    SortBins.run(args)
    Aggregations.run(args)
    logger.info("Finished all Map/Reduce jobs")

  }