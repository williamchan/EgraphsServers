package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils.Conversions._
import utils._
import services.db.{TransactionSerializable, DBSession}
import services.AppConfig
import controllers.routes.WebsiteControllers.postMastheadAdmin
import controllers.routes.WebsiteControllers.getMastheadAdmin
import models.MastheadStore
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.Date
import play.mvc.Http.HeaderNames
import models.enums.{PublishedStatus, CallToActionType}
import play.api.test.FakeHeaders
import play.api.mvc.MultipartFormData.FilePart
import scala.Some
import models.Masthead


class PostMastheadAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedMultipartFormResourceTests with AdminProtectedMultipartFormResourceTests {
  protected def routeUnderTest = postMastheadAdmin
  protected def db = AppConfig.instance[DBSession]
  protected def controllerMethod = controllers.WebsiteControllers.postMastheadAdmin

  def mastheadStore = AppConfig.instance[MastheadStore]

  postMastheadAdmin.url should "reject empty headlines" in new EgraphsTestApplication {
    val mastheadId = newMastheadId
    db.connected(TransactionSerializable) {
      val Some(result) = performRequest(mastheadId = mastheadId.toString, headline = "", adminId = admin.id)
      status(result) should be(SEE_OTHER)
      redirectLocation(result) should be(Some(getMastheadAdmin(mastheadId).url))
    }
  }

  it should "accept a complete request" in new EgraphsTestApplication {
    val mastheadId = newMastheadId
    val headline =  TestData.generateFullname()
    db.connected(TransactionSerializable) {
      val Some(result) = performRequest(mastheadId = mastheadId.toString, headline = headline, adminId = admin.id)

      status(result) should be(SEE_OTHER)
      redirectLocation(result) should be (Some(getMastheadAdmin(mastheadId).url))

      val newMasthead = mastheadStore.get(mastheadId)

      newMasthead.headline should be(headline)
    }
  }

  private def performRequest(mastheadId: String,
                             headline: String = "a headline",
                             callToActionTypeString: String = CallToActionType.SearchBox.name,
                             publishedStatusString: String  = PublishedStatus.Published.name,
                             adminId: Long): Option[play.api.mvc.Result] = {

    val tempFile = File.createTempFile("afakefile", "txt")
    FileUtils.writeStringToFile(tempFile, (new Date()).toString)

    val fakeImageFile = Seq(FilePart("testFile", "text.txt", Some("text/plain"),
      play.api.libs.Files.TemporaryFile(tempFile)))

    val body = MultipartFormData[TemporaryFile](
      Map("mastheadId" -> Seq(mastheadId),
        "name" -> Seq("name"),
        "headline" -> Seq(headline),
        "subtitle" -> Seq("subtitle"),
        "publishedStatusString" -> Seq(publishedStatusString),
        "callToActionTypeString" -> Seq(callToActionTypeString),
        "callToActionTarget" -> Seq("target"),
        "callToActionText" -> Seq("text")
      ),
      fakeImageFile,
      badParts = Seq())

     Some(controllers.WebsiteControllers.postMastheadAdmin(
       FakeRequest(POST,
        postMastheadAdmin().url,
        FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))),
        body = body
      ).withAuthToken.withAdmin(adminId)))
  }

  private def newMastheadId : Long = {
    db.connected(TransactionSerializable) {
      Masthead(headline = TestData.generateFullname()).save().id
    }
  }

}
