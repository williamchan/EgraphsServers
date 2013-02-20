package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import utils.{TestData, CsrfProtectedResourceTests, EgraphsUnitTest}
import services.db.{TransactionSerializable, DBSession}
import services.AppConfig
import controllers.routes.WebsiteControllers.postMastheadAdmin
import models.{Administrator, Masthead, MastheadStore}
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.Date
import play.api.mvc.MultipartFormData.FilePart
import play.mvc.Http.HeaderNames


class PostMastheadAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests {
  protected def routeUnderTest = postMastheadAdmin
  protected def db = AppConfig.instance[DBSession]

  def mastheadStore = AppConfig.instance[MastheadStore]

  routeName(postMastheadAdmin) should "reject empty headlines" in new EgraphsTestApplication {

    db.connected(TransactionSerializable) {
      val adminId = createAdmin.id
      val Some(result) = performRequest(mastheadId = newMastheadId.toString, headline = "", adminId)
      status(result) should be(SEE_OTHER)
    }
  }

  private def performRequest(mastheadId: String,  headline: String, adminId: Long): Option[play.api.mvc.Result] = {
    val tempFile = File.createTempFile("landingPageImage", "jpg")
    FileUtils.writeStringToFile(tempFile, (new Date()).toString)
    val filename = "test.jpg"
    val fakeImageFile = Seq(FilePart("landingPageImage", filename, Some("image/jpeg"),
      play.api.libs.Files.TemporaryFile(tempFile)))

    val postBody = MultipartFormData[TemporaryFile](
      Map("mastheadId" -> Seq(mastheadId), "headline" -> Seq(headline)),
      fakeImageFile,
      badParts = Seq(),
      missingFileParts = Seq()
    )
    val request = FakeRequest()

    routeAndCall(
      request.copy(POST,
        controllers.routes.WebsiteControllers.postMastheadAdmin().url,
        FakeHeaders(Map(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))),
        postBody
      ).withAuthToken.withAdmin(adminId)
    )
  }

  private def newMastheadId : Long = {
    db.connected(TransactionSerializable) {
      Masthead(headline = TestData.generateFullname()).save().id
    }
  }

  private def createAdmin : Administrator = {TestData.newSavedAdministrator()}
}
