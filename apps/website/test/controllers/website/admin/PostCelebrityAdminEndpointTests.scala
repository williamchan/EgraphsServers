package controllers.website.admin

import utils.{AdminProtectedMultipartFormResourceTests, CsrfProtectedMultipartFormResourceTests, EgraphsUnitTest, TestData}
import services.AppConfig
import models.CelebrityStore
import services.db.TransactionSerializable
import utils.FunctionalTestUtils.Conversions._
import play.api.mvc.{Result, MultipartFormData}
import play.api.libs.Files.TemporaryFile
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import scala.Some
import play.mvc.Http.HeaderNames


class PostUpdateCelebrityAdminEndpointTests extends PostCelebrityAdminEndpointTests
  with EgraphsUnitTest
  with CsrfProtectedMultipartFormResourceTests
  with AdminProtectedMultipartFormResourceTests
{
  protected def controllerMethod = controllers.WebsiteControllers.postCelebrityAdmin(0)
  protected def routeUnderTest = controllers.routes.WebsiteControllers.postCelebrityAdmin(0)
  def celebrityStore = AppConfig.instance[CelebrityStore]

  routeUnderTest.url should "update a celebrity" in new EgraphsTestApplication {
    val (celebrity, account) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account)
    }

    db.connected(TransactionSerializable) {
      val newName = TestData.generateFullname()
      val body = createBody(email = account.email, publicName = newName)

      val Some(result) = performRequest(body, adminId = admin.id, celebId = celebrity.id)
      val updatedCeleb = celebrityStore.get(celebrity.id)

      status(result) should be(SEE_OTHER)
      redirectLocation(result).getOrElse("").contains("?action=preview") should be(true)
      updatedCeleb.publicName should be(newName)
    }
  }


  private def performRequest(body: MultipartFormData[TemporaryFile], adminId: Long, celebId: Long) : Option[Result] = {
    Some(controllers.WebsiteControllers.postCelebrityAdmin(celebId)(
      FakeRequest(POST,
        routeUnderTest.url,
        FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))),
        body = body
      ).withAuthToken.withAdmin(adminId)))
  }
}
