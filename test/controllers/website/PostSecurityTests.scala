package controllers.website

import org.junit.Test
import play.mvc.Http.Response
import play.test.FunctionalTest
import FunctionalTest._
import services.Utils

class PostSecurityTests extends FunctionalTest {

  @Test
  def testPostControllersRequireAuthenticityToken() {
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postLoginAdmin", Map[String, String]("authenticityCheck" -> "fail")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postLogoutAdmin", Map[String, String]("authenticityCheck" -> "fail")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postAccountAdmin", Map[String, String]("authenticityCheck" -> "fail")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postCelebrityAdmin", Map[String, String]("authenticityCheck" -> "fail")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postCelebrityProductAdmin", Map[String, String]("authenticityCheck" -> "fail", "celebrityId" -> "1")).url))
    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postOrderAdmin", Map[String, String]("authenticityCheck" -> "fail", "orderId" -> "1")).url))

    assertAuthenticityTokenIsRequired(POST(Utils.lookupUrl("WebsiteControllers.postBuyProduct", Map[String, String]("authenticityCheck" -> "fail", "celebrityUrlSlug" -> "1", "productUrlSlug" -> "1")).url))
  }

  private def assertAuthenticityTokenIsRequired(response: Response) {
    assertStatus(403, response)
    assertContentMatch("Bad authenticity token", response)
  }
}
