package controllers.api

import org.specs2.mock.Mockito
import play.mvc.Http.Request
import play.mvc.Controller
import org.scalatest.matchers.ShouldMatchers
import play.mvc.results.Forbidden
import play.test.UnitFlatSpec
import services.Time
import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenario}
import models._
import services.http.CelebrityAccountRequestFilters
import utils.{MockControllerMethod, TestConstants}

class GetCelebrityApiEndpointTests extends UnitFlatSpec with Mockito with ShouldMatchers
{

  private def testController(reqFilters: CelebrityAccountRequestFilters): GetCelebrityApiEndpoint = {
    new Controller with GetCelebrityApiEndpoint {
      override def controllerMethod = MockControllerMethod
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

class GetCelebrityApiEndpointFunctionalTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  @Test
  def testGettingACelebrity() {
    // Set up the scenario
    runScenario("Will-Chan-is-a-celebrity")

    // Execute the request
    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me")

    // Test expectations
    assertIsOk(response)

    val json = Serializer.SJSON.in[Map[String, AnyRef]](getContent(response))

    assertNotNull(json("id"))
    assertEquals("Wizzle", json("publicName"))
    assertEquals("Wizzle", json("urlSlug"))
    assertEquals("William", json("firstName"))
    assertEquals("Chan", json("lastName"))
    assertEquals(false, json("isLeftHanded"))
    assertEquals(EnrollmentStatus.NotEnrolled.value, json("enrollmentStatus"))

    // These conversions will fail if they're not Longs
    Time.fromApiFormat(json("created").toString)
    Time.fromApiFormat(json("updated").toString)

    assertEquals(9, json.size)
  }

  @Test
  def testGettingACelebrityWithIncorrectCredentialsFails() {
    // Set up the scenario
    runScenario("Will-Chan-is-a-celebrity")

    // Assemble the request
    val req = newRequest()
    req.user = "wchan83@egraphs.com"
    req.password = "wrongwrongwrong"

    // Execute the request
    val response = GET(req, TestConstants.ApiRoot + "/celebrities/me")
    assertEquals(403, response.status)
  }
}
