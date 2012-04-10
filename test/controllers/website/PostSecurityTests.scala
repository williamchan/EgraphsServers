package controllers.website

import org.junit.{After, Test}
import play.mvc.Http.Response
import play.Play
import play.test.FunctionalTest
import FunctionalTest._
import services.Utils
import utils.TestData

class PostSecurityTests extends FunctionalTest {

  @After
  def after() {
    TestData.passAuthenticityCheck = (Play.id == "test")
  }

  @Test
  def testPostControllersRequireAuthenticityToken() {
    TestData.passAuthenticityCheck = false

    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postAdminLogin").url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postAdminLogout").url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postCelebrity").url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postCelebrityProduct", Map[String, String]("celebrityId" -> "1")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postBuyProduct", Map[String, String]("celebrityUrlSlug" -> "1", "productUrlSlug" -> "1")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postBuyDemoProduct", Map[String, String]("celebrityUrlSlug" -> "1", "productUrlSlug" -> "1")).url))
  }

  private def assertAuthenticityTokenIsRequired(response: Response) {
    assertStatus(403, response)
    assertContentMatch("Bad authenticity token", response)
  }
}
