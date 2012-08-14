package services

import utils._
import models.CelebrityStore
import scala.Some
import controllers.WebsiteControllers

class UtilsTests extends EgraphsUnitTest
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest
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

  "lookupUrl" should "find controllers that exist" in {
    val params = Map("celebrityUrlSlug" -> "Wizzle", "productUrlSlug" -> "Herp")
    Utils.lookupUrl("WebsiteControllers.getStorefrontChoosePhotoCarousel", params).url should be ("/Wizzle/photos/Herp")
  }

  "absoluteUrl" should "prepend the url with application.baseUrl" in {
    val action = WebsiteControllers.reverse(WebsiteControllers.getStorefrontChoosePhotoCarousel("Wizzle", "2010-Starcraft-2-Championships"))
    val myUtils = new Utils(Utils.properties("application.baseUrl" -> "https://www.mysite.com/"))
    myUtils.absoluteUrl(action) should be("https://www.mysite.com/Wizzle/photos/2010-Starcraft-2-Championships")
  }

  "composeUrl" should "include just one slash when joining a base URL to a relative URL" in {
    Utils.composeUrl("https://www.egraphs.com/", "/login") should be("https://www.egraphs.com/login")
    Utils.composeUrl("https://www.egraphs.com/", "login")  should be("https://www.egraphs.com/login")
    Utils.composeUrl("https://www.egraphs.com" , "/login") should be("https://www.egraphs.com/login")
    Utils.composeUrl("https://www.egraphs.com" , "login")  should be("https://www.egraphs.com/login")
  }

  "requiredConfigurationProperty" should "get existing configuration properties fine" in {
    Utils.requiredConfigurationProperty("application.name")
  }

  it should "throw an exception for configuration properties that don't exist" in {
    evaluating { Utils.requiredConfigurationProperty("herp") } should produce[IllegalArgumentException]
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

  "pageQuery" should "return paged Query and total number of results if specified" in {
    val celebStore = AppConfig.instance[CelebrityStore]

    TestData.newSavedCelebrity()
    TestData.newSavedCelebrity()
    TestData.newSavedCelebrity()

    val pagedQuery0 = Utils.pagedQuery(select = celebStore.getCelebrityAccounts, page = 1, pageLength = 2, withTotal = true)
    pagedQuery0._1.size should be(2)
    pagedQuery0._2 should be(1)
    pagedQuery0._3.get should be(3)

    val pagedQuery1 = Utils.pagedQuery(select = celebStore.getCelebrityAccounts, page = 2, pageLength = 2, withTotal = true)
    pagedQuery1._1.size should be(1)
    pagedQuery1._3.get should be(3)

    val pagedQuery2 = Utils.pagedQuery(select = celebStore.getCelebrityAccounts, page = 3, pageLength = 2, withTotal = true)
    pagedQuery2._1.size should be(0)
    pagedQuery2._3.get should be(3)

    Utils.pagedQuery(select = celebStore.getCelebrityAccounts, page = 0, pageLength = 3, withTotal = false)._3 should be(None)
  }

  "Enum" should "find Values for strings in its apply method" in {
    object TestEnum extends Utils.Enum {
      case class EnumVal(name: String) extends Value

      val Value1 = EnumVal("Value1")
      val Value2 = EnumVal("Value2")
    }

    TestEnum("Value1") should be(Some(TestEnum.Value1))
    TestEnum("Value2") should be(Some(TestEnum.Value2))
    TestEnum("Herp")   should be(None)
  }

  "Enum" should "not allow duplicate value entries" in {
    evaluating {
      object TestEnum extends Utils.Enum {
        case class EnumVal(name: String) extends Value

        val Value1 = EnumVal("Value1")
        val Value2 = EnumVal("Value1")
      }
      // Accessing TestEnum to force evaluation to catch require exception
      TestEnum("herp")
    } should produce[IllegalArgumentException]
  }

}