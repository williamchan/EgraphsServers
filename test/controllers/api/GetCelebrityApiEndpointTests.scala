package controllers.api

import org.specs2.mock.Mockito
import play.test.UnitFlatSpec
import play.mvc.Http.Request
import services.http.CelebrityAccountRequestFilters
import models.{Account, Celebrity}
import play.mvc.Controller
import org.scalatest.matchers.ShouldMatchers
import play.mvc.results.Forbidden

class GetCelebrityApiEndpointTests extends UnitFlatSpec with Mockito with ShouldMatchers
{

  private def testController(reqFilters: CelebrityAccountRequestFilters) = {
    new Controller with GetCelebrityApiEndpoint {
      override def celebFilters = reqFilters
    }
  }

  "GetCelebrityApiEndpoint" should "serialize celebrity json when filters passed" in {
    implicit val request = mock[Request]
    val account = mock[Account]
    val celeb = mock[Celebrity]
    val mockFilters = mock[CelebrityAccountRequestFilters]

    mockFilters.requireCelebrityAccount(any)(any) answers { case Array(callback: Function2[Account, Celebrity, Any], req) =>
      callback(account, celeb)
    }

    celeb.renderedForApi returns (Map("celebrityKeys" -> "celebrityValues"))

    testController(mockFilters).getCelebrity.asInstanceOf[String] should be ("""{"celebrityKeys":"celebrityValues"}""")
  }
  
  it should "respect the requireCelebrityAccount filter" in {
    implicit val request = mock[Request]
    val mockFilters = mock[CelebrityAccountRequestFilters]
    val filterResult = new Forbidden("failed the filter")
    
    mockFilters.requireCelebrityAccount(any)(any) returns (filterResult)
    
    testController(mockFilters).getCelebrity should be (filterResult)
  }
}