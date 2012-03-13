package services

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import org.scalatest.BeforeAndAfterEach
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter}

class UtilsTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  class Closeable {
    var closed = false

    def close() = {
      closed = true
    }
  }

  "toMap" should "correctly make a map from a list" in {
    val map = Utils.toMap(List(1, 2), key=(theInt: Int) => theInt.toString)
    map should be (Map("1" -> 1, "2" -> 2))
  }
  
  "toOption" should "return Some for non-empty String and None for empty String" in {
    Utils.toOption("") should be(None)
    Utils.toOption(" ") should be(Some(" "))
    Utils.toOption("a") should be(Some("a"))
  }

  "closing" should "close a closeable resource on success" in {
    val closeable = new Closeable()

    Utils.closing(closeable) { closeableIn =>
      closeable.closed should be (false)
    }

    closeable.closed should be (true)
  }

  it should "close a closeable resource when exceptions are thrown" in {
    val closeable = new Closeable()

    def throwsExceptionMidUsage = {
      Utils.closing(closeable) { closeableIn =>
        throw new IllegalStateException("herp")
      }
    }

    evaluating { throwsExceptionMidUsage } should produce [IllegalStateException]
    closeable.closed should be (true)
  }

  "lookupUrl" should "find controllers that exist" in {
    val params = Map("celebrityUrlSlug" -> "Wizzle", "productUrlSlug" -> "Herp")
    Utils.lookupUrl("WebsiteControllers.postBuyProduct", params).url should be ("/Wizzle/Herp/buy")

  }

  "requiredConfigurationProperty" should "get existing configuration properties fine" in {
    Utils.requiredConfigurationProperty("application.name")
  }

  it should "throw an exception for configuration properties that don't exist" in {
    evaluating { Utils.requiredConfigurationProperty("herp") } should produce[IllegalArgumentException]
  }
}