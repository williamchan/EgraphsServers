package services.http

import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.FakeRequest
import utils.EgraphsUnitTest
import services.config.ConfigFileProxy
import play.api.test.Helpers._

class HttpsFilterTests extends EgraphsUnitTest {

  "HttpsFilter" should "redirect insecure http requests to https when httpsOnly is true" in {
    val result = resultFromTestRequest(protocol="http", httpsOnly=true)
    
    status(result) should be (SEE_OTHER)
    redirectLocation(of=result) should be (Some("https://" + urlToTest))
  }
 
  it should "serve https requests" in {
   val result = resultFromTestRequest(protocol="https", httpsOnly=true)
 
   status(result) should be (OK)
  }
 
  it should "serve http requests when httpsOnly is false" in {
    val result = resultFromTestRequest(protocol="http", httpsOnly=false)   
 
    status(result) should be (OK)
  }
 
  private val urlToTest = "www.egraphs.com"
 
  private def resultFromTestRequest(protocol: String, httpsOnly: Boolean): Result = {
    val request = FakeRequest(GET, urlToTest).withHeaders("x-forwarded-proto" -> protocol)
    val mockConfig = mock[ConfigFileProxy]
    mockConfig.applicationHttpsOnly returns httpsOnly
    
    val httpsFilter = new HttpsFilter(mockConfig)
    val action = httpsFilter(Action { request => Ok } )
    
    action(request)
  }

}
