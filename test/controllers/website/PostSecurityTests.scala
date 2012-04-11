package controllers.website

import org.junit.Test
import play.mvc.Http.Response
import play.test.FunctionalTest
import FunctionalTest._
import services.Utils

class PostSecurityTests extends FunctionalTest {

  @Test
  def testPostControllersRequireAuthenticityToken() {
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postAdminLogin", Map[String, String]("authenticityCheck" -> "fail")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postAdminLogout", Map[String, String]("authenticityCheck" -> "fail")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postCelebrity", Map[String, String]("authenticityCheck" -> "fail")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postCelebrityProduct", Map[String, String]("authenticityCheck" -> "fail", "celebrityId" -> "1")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postBuyProduct", Map[String, String]("authenticityCheck" -> "fail", "celebrityUrlSlug" -> "1", "productUrlSlug" -> "1")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postBuyDemoProduct", Map[String, String]("authenticityCheck" -> "fail", "celebrityUrlSlug" -> "1", "productUrlSlug" -> "1")).url))
  }

  private def assertAuthenticityTokenIsRequired(response: Response) {
    assertStatus(403, response)
    assertContentMatch("Bad authenticity token", response)
  }
}
