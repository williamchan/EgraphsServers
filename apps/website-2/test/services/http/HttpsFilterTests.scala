package services.http

import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.FakeRequest
import utils.EgraphsUnitTest
import services.config.ConfigFileProxy

//TODO: PLAY20 - having trouble getting the logic in HttpsFilter to actually execute in these tests
class HttpsFilterTests extends EgraphsUnitTest {

  "HttpsFilter" should "redirect insecure http requests to https when httpsOnly is true" in (pending)

  "HttpsFilter" should "serve https requests" in (pending)

  "HttpsFilter" should "serve http requests when httpsOnly is false" in (pending)
  
//  "HttpsFilter" should "redirect insecure http requests to https when httpsOnly is true" in {
//    val request: Request[AnyContent] = FakeRequest("GET", "www.egraphs.com").withHeaders("x-forwarded-proto" -> "http")
//    val mockConfig = mock[ConfigFileProxy]
//    mockConfig.applicationHttpsOnly returns true
//    
//    val httpsFilter = new HttpsFilter(mockConfig)
//    val action = httpsFilter(Action { request => Ok } )
//    println("action " + action)
//  }
//
//  "HttpsFilter" should "serve https requests" in {
//    val request: Request[AnyContent] = FakeRequest("GET", "www.egraphs.com").withHeaders("x-forwarded-proto" -> "https")
//    val mockConfig = mock[ConfigFileProxy]
//    mockConfig.applicationHttpsOnly returns true
//
//    val httpsFilter = new HttpsFilter(mockConfig)
//    val action = httpsFilter(Action { request => Ok } )
//    println("action " + action)
//  }
//
//  "HttpsFilter" should "serve http requests when httpsOnly is false" in {
//    val request: Request[AnyContent] = FakeRequest("GET", "www.egraphs.com").withHeaders("x-forwarded-proto" -> "http")
//    val mockConfig = mock[ConfigFileProxy]
//    mockConfig.applicationHttpsOnly returns false
//    
//    val httpsFilter = new HttpsFilter(mockConfig)
//    val action = httpsFilter(Action { request => Ok } )
//    println("action " + action)
//  }

}
