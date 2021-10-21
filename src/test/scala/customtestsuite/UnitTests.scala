package scala.customtestsuite

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.hadoop.io.Text
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

import java.util
import java.util.List
import java.util.regex.Pattern

class UnitTests extends AnyFunSuite {

  test("Unit test to check successful loading of configuration") {
    val config: Config = ConfigFactory.load("application.conf")
    val duration = config.getInt("randomLogGenerator.DurationMinutes")
    val count = config.getInt("randomLogGenerator.MaxCount")
    assert(count > 0 || duration > 0)
  }

  test("Unit test to check pattern matching") {
    val config: Config = ConfigFactory.load("application.conf")
    val testingString = "20:54:56.986 [scala-execution-context-global-181] ERROR HelperUtils.Parameters$ - NT1#6aT6sY6uB9wG7vbe0F8wL9m\"Irsj,"
    val keywordPatternMatcher = Pattern.compile(config.getString("randomLogGenerator.taskConfigs.wordCount.detectTypeInstancesOf")).matcher(testingString)
    assert(keywordPatternMatcher.find())
  }

  test("Unit test to check pattern matching negative case") {
    val config: Config = ConfigFactory.load("application.conf")
    val testingString = "20:54:56.986 [scala-execution-context-global-181] RANDOM HelperUtils.Parameters$ - NT1#6aT6sY6uB9wG7vbe0F8wL9m\"Irsj,"
    val keywordPatternMatcher = Pattern.compile(config.getString("randomLogGenerator.taskConfigs.wordCount.detectTypeInstancesOf")).matcher(testingString)
    assert(!keywordPatternMatcher.find())
  }

  test("Unit test to check bin generation") {
    val word = new Text()
    val testingString = "20:54:56.986 [scala-execution-context-global-181] ERROR HelperUtils.Parameters$ - NT1#6aT6sY6uB9wG7vbe0F8wL9m\"Irsj,"
    val nextMinuteBin = String.format("%02d", testingString.substring(3, 5).toInt + 1)
    val hourBin = testingString.substring(0, 3)
    if(nextMinuteBin == "60" && hourBin == "24") {
      word.set(testingString.substring(0, 5) + "-" + "00:00")
    }
    else if(nextMinuteBin == "60") {
      word.set(testingString.substring(0, 5) + "-" + String.format("%02d", testingString.substring(0, 2).toInt + 1) + ":00")
    }
    else
      word.set(testingString.substring(0, 5) + "-" + testingString.substring(0, 3) + String.format("%02d", testingString.substring(3, 5).toInt + 1))

    assert(word.toString == "20:54-20:55")
  }

  test("Unit test to check bin generation edge case for minutes only") {
    val word = new Text()
    val testingString = "20:59:56.986 [scala-execution-context-global-181] ERROR HelperUtils.Parameters$ - NT1#6aT6sY6uB9wG7vbe0F8wL9m\"Irsj,"
    val nextMinuteBin = String.format("%02d", testingString.substring(3, 5).toInt + 1)
    val hourBin = testingString.substring(0, 3)
    if(nextMinuteBin == "60" && hourBin == "24") {
      word.set(testingString.substring(0, 5) + "-" + "00:00")
    }
    else if(nextMinuteBin == "60") {
      word.set(testingString.substring(0, 5) + "-" + String.format("%02d", testingString.substring(0, 2).toInt + 1) + ":00")
    }
    else
      word.set(testingString.substring(0, 5) + "-" + testingString.substring(0, 3) + String.format("%02d", testingString.substring(3, 5).toInt + 1))

    assert(word.toString == "20:59-21:00")
  }

  test("Unit test to check bin generation edge case for minutes and hours") {
    val word = new Text()
    val testingString = "23:59:56.986 [scala-execution-context-global-181] ERROR HelperUtils.Parameters$ - NT1#6aT6sY6uB9wG7vbe0F8wL9m\"Irsj,"
    val nextMinuteBin = String.format("%02d", testingString.substring(3, 5).toInt + 1)
    val hourBin = testingString.substring(0, 2)
    if(nextMinuteBin == "60" && hourBin == "23") {
      word.set("23:59-00:00")
    }
    else if(nextMinuteBin == "60") {
      word.set(testingString.substring(0, 5) + "-" + String.format("%02d", testingString.substring(0, 2).toInt + 1) + ":00")
    }
    else
      word.set(testingString.substring(0, 5) + "-" + testingString.substring(0, 3) + String.format("%02d", testingString.substring(3, 5).toInt + 1))

    assert(word.toString == "23:59-00:00")
  }

  test("Unit test to check amount of digits in time interval to be maintained at 2") {
    val word = new Text()
    val testingString = "01:04:56.986 [scala-execution-context-global-181] ERROR HelperUtils.Parameters$ - NT1#6aT6sY6uB9wG7vbe0F8wL9m\"Irsj,"
    val nextMinuteBin = String.format("%02d", testingString.substring(3, 5).toInt + 1)
    val hourBin = testingString.substring(0, 2)
    if(nextMinuteBin == "60" && hourBin == "23") {
      word.set("23:59-00:00")
    }
    else if(nextMinuteBin == "60") {
      word.set(testingString.substring(0, 5) + "-" + String.format("%02d", testingString.substring(0, 2).toInt + 1) + ":00")
    }
    else
      word.set(testingString.substring(0, 5) + "-" + testingString.substring(0, 3) + String.format("%02d", testingString.substring(3, 5).toInt + 1))

    assert(word.toString == "01:04-01:05")
  }

}