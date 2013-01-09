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
import utils.EgraphsUnitTest
import utils.TestData
import play.api.mvc.SimpleResult
import play.api.mvc.Result
import play.api.mvc.AsyncResult
import play.api.mvc.PlainResult
import org.joda.time.DateTimeConstants
import models.VideoAssetCelebrityStore
import org.apache.commons.io.FileUtils
import java.util.Date

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
    FileUtils.writeStringToFile(tempFile, (new Date()).toString)

    val filename = "test.mp4"
    val nonFiles = Map("celebrityId" -> Seq(celebrityId.toString))
    val fakeVideoPart = FilePart("video", filename, Some("video/mp4"),
      play.api.libs.Files.TemporaryFile(tempFile))

    val fakeVideoFile = Seq(fakeVideoPart)
    val postBody = MultipartFormData[TemporaryFile](nonFiles, fakeVideoFile, Seq(), Seq())

    val Some(result) = routeAndCall(FakeRequest(POST, controllers.routes.ApiControllers.postVideoAsset.url,
      FakeHeaders(Map(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))), postBody).withHeaders(auth.toHeader))

    myStatus(result) should be(OK)

    val blob: Blobs = AppConfig.instance[Blobs]
    val videoAssetCelebrityStore: VideoAssetCelebrityStore = AppConfig.instance[VideoAssetCelebrityStore]

    val maybeVideoAsset = db.connected(TransactionSerializable) {
      videoAssetCelebrityStore.getVideoAssetByCelebrityId(celebrityId)
    }

    maybeVideoAsset match {
      case None => fail("There is no video asset associated with celebrityId " + celebrityId)
      case Some(videoAsset) => {
        val videoKey = "videoassets/" + videoAsset.id + "/" + filename
        val maybeFileLocation = blob.getUrlOption(key = videoKey)
        maybeFileLocation.isDefined should be(true)
      }
    }
  }

  private def myStatus(result: Result): Int = {
    result match {
      case asyncResult: AsyncResult => myStatus(asyncResult.result.await(30 * DateTimeConstants.MILLIS_PER_SECOND).get)
      case anythingElse => status(result)  
    }
  }
}