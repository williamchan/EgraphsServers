package controllers.api

import java.io.File

import controllers.routes.ApiControllers.postVideoAsset
import play.api.http.HeaderNames
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.MultipartFormData
import play.api.test.Helpers.OK
import play.api.test.Helpers.POST
import play.api.test.Helpers.routeAndCall
import play.api.test.Helpers.status
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import services.blobs.Blobs
import services.db.DBSession
import services.db.TransactionSerializable
import services.http.BasicAuth
import services.AppConfig
import utils.FunctionalTestUtils.{ runFreshScenarios, willChanRequest }
import utils.EgraphsUnitTest
import utils.TestData

class PostVideoAssetApiEndpointTests extends EgraphsUnitTest with ProtectedCelebrityResourceTests {
  protected override def routeUnderTest = postVideoAsset
  protected def db = AppConfig.instance[DBSession]

  it should "accept multipartFormData, respond with OK, and verify file creation in the blobstore" in new EgraphsTestApplication {

    val password = "bubble toes"

    val account = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      celebrity.account.withPassword(password).right.get.save()
    }

    val celebrityId = account.celebrityId.get
    val auth = BasicAuth.Credentials(account.email, password)

    val tempFile = File.createTempFile("videoFile", "mp4")

    val filename = "test.mp4"
    val nonFiles = Map("celebrityId" -> Seq(celebrityId.toString))
    val fakeVideoPart = FilePart("video", filename, Some("video/mp4"),
      play.api.libs.Files.TemporaryFile(tempFile))

    val fakeVideoFile = Seq(fakeVideoPart)
    val postBody = MultipartFormData[TemporaryFile](nonFiles, fakeVideoFile, Seq(), Seq())

    val Some(result) = routeAndCall(FakeRequest(POST, "/api/1.0/celebrities/me/videoasset",
      FakeHeaders(Map(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))), postBody).withHeaders(auth.toHeader))

    status(result) should be(OK)

    val blob: Blobs = AppConfig.instance[Blobs]
    val videoKey = "videos/" + celebrityId + "/" + filename
    val maybeFileLocation = blob.getUrlOption(key = videoKey)

    maybeFileLocation.isDefined should be(true)
  }
}