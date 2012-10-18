package services.http

import scala.Either
import play.api.mvc.Result
import utils.EgraphsUnitTest

//TODO: PLAY20: this should probably be deleted along with HttpsFilters.scala 
class HttpsFilterTests extends EgraphsUnitTest {

//  "HttpsFilter" should "redirect insecure http requests to https when httpsOnly is true" in {
//    implicit val request = FunctionalTestUtils.createRequest(secure = false)
//    val playConfig = FunctionalTestUtils.createProperties(HttpsFilter.httpsOnlyProperty, "true")
//
//    val result: Either[Result, Any] = new HttpsFilter(playConfig).apply(1)
//    result.isLeft should be(true)
//    result.left.get.asInstanceOf[Redirect].url should be ("https://www.egraphs.com/")
//  }
//
//  "HttpsFilter" should "serve https requests" in {
//    implicit val request = FunctionalTestUtils.createRequest(secure = true)
//    val playConfig = FunctionalTestUtils.createProperties(HttpsFilter.httpsOnlyProperty, "true")
//
//    val result: Either[Result, Any] = new HttpsFilter(playConfig).apply(1)
//    result.isRight should be(true)
//    result.right.get should be(1)
//  }
//
//  "HttpsFilter" should "serve http requests when httpsOnly is false" in {
//    implicit val request = FunctionalTestUtils.createRequest(secure = false)
//    val playConfig = FunctionalTestUtils.createProperties(HttpsFilter.httpsOnlyProperty, "false")
//
//    val result: Either[Result, Any] = new HttpsFilter(playConfig).apply(1)
//    result.isRight should be(true)
//    result.right.get should be(1)
//  }

}
