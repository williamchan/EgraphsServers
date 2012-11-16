package services

import utils._
import controllers.WebsiteControllers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import services.config.ConfigFileProxy

@RunWith(classOf[JUnitRunner])
class UtilsTests extends EgraphsUnitTest
{
  class Closeable {
    var closed = false

    def close() {
      closed = true
    }
  }

  "toMap" should "correctly make a map from a list" in {
    val map = Utils.toMap(List(1, 2), key=(theInt: Int) => theInt.toString)
    map should be (Map("1" -> 1, "2" -> 2))
  }

  "toOption" should "return Some for non-empty String and None for empty String" in {
    Utils.toOption(null) should be(None)
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

  "saveToFile" should "save bytes parameter to a file" in {
    val file = TempFile.named("helloworld.txt")
    try {
      Utils.saveToFile("hello world".getBytes, file)
      TestHelpers.getStringFromFile(file) should be("hello world")
    } finally {
      file.delete()
    }
  }


  "pageQuery" should "return paged Query and total number of results if specified" in (pending)
// This test adds value as a pure unit test, but while talking to a real DB it is stupid since it isn't able to play well with other tests.
//  {
//    val celebStore = AppConfig.instance[CelebrityStore]
//
//    TestData.newSavedCelebrity()
//    TestData.newSavedCelebrity()
//    TestData.newSavedCelebrity()
//
//    val pagedQuery0 = Utils.pagedQuery(select = celebStore.getCelebrityAccounts, page = 1, pageLength = 2, withTotal = true)
//    pagedQuery0._1.size should be(2)
//    pagedQuery0._2 should be(1)
//    pagedQuery0._3.get should be >= (3)
//
//    val pagedQuery1 = Utils.pagedQuery(select = celebStore.getCelebrityAccounts, page = 2, pageLength = 2, withTotal = true)
//    pagedQuery1._1.size should be(1)
//    pagedQuery1._3.get should be >= (3)
//
//    val pagedQuery2 = Utils.pagedQuery(select = celebStore.getCelebrityAccounts, page = 3, pageLength = 2, withTotal = true)
//    pagedQuery2._1.size should be(0)
//    pagedQuery2._3.get should be >= (3)
//
//    Utils.pagedQuery(select = celebStore.getCelebrityAccounts, page = 0, pageLength = 3, withTotal = false)._3 should be(None)
//  }

  "Enum" should "find Values for strings in its apply method" in {
    object TestEnum extends Enum {
      case class EnumVal(name: String) extends Value

      val Value1 = EnumVal("Value1")
      val Value2 = EnumVal("Value2")
    }

    TestEnum("Value1") should be(Some(TestEnum.Value1))
    TestEnum("Value2") should be(Some(TestEnum.Value2))
    TestEnum("Herp")   should be(None)
  }

  // TODO: PLAY20 migration. Make enums actually disallow duplicate value entries.
  "Enum" should "not allow duplicate value entries" in (pending) /*{
    evaluating {
      object TestEnum extends Enum {
        case class EnumVal(name: String) extends Value

        val Value1 = EnumVal("Value1")
        val Value2 = EnumVal("Value1")
      }
      // Accessing TestEnum to force evaluation to catch require exception
      TestEnum("herp")
    } should produce[IllegalArgumentException]
  }*/

}