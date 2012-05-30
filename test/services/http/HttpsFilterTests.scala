package services.http

import java.util.Properties
import play.test.FunctionalTest
import scala.Either
import utils.EgraphsUnitTest
import play.mvc.results.{Redirect, Result}

class HttpsFilterTests extends EgraphsUnitTest {

  "HttpsFilter" should "redirect insecure http requests to https when httpsOnly is true" in {
    implicit val request = FunctionalTest.newRequest()
    request.secure = false
    request.host = "www.egraphs.com"
    request.url = "/"

    val playConfig = new Properties
    playConfig.setProperty(HttpsFilter.httpsOnlyProperty, "true")

    val result: Either[Result, Any] = new HttpsFilter(playConfig).apply(1)
    result.isLeft should be(true)
    result.left.get.asInstanceOf[Redirect].url should be ("https://www.egraphs.com/")
  }

  "HttpsFilter" should "serve http requests" in {
    implicit val request = FunctionalTest.newRequest()
    request.secure = true
    request.host = "www.egraphs.com"
    request.url = "/"

    val playConfig = new Properties
    playConfig.setProperty(HttpsFilter.httpsOnlyProperty, "true")

    val result: Either[Result, Any] = new HttpsFilter(playConfig).apply(1)
    result.isRight should be(true)
    result.right.get should be(1)
  }

  "HttpsFilter" should "serve http requests when httpsOnly is false" in {
    implicit val request = FunctionalTest.newRequest()
    request.secure = false
    request.host = "www.egraphs.com"
    request.url = "/"

    val playConfig = new Properties
    playConfig.setProperty(HttpsFilter.httpsOnlyProperty, "false")

    val result: Either[Result, Any] = new HttpsFilter(playConfig).apply(1)
    result.isRight should be(true)
    result.right.get should be(1)
  }

}
