
import HelperUtils.{CreateLogger, ObtainConfigReference}
//import WordCount
//import MaxCharacters
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

class RunJobs
object RunJobs:
  val logger = CreateLogger(classOf[RunJobs])

  def main(args: Array[String]): Unit = {
    logger.info("Starting Map/Reduce jobs")
//    WordCount.main(args)
//    MaxCharacters.main(args)
    SortBins.main(args)
    logger.info("Finished all Map/Reduce jobs")

  }