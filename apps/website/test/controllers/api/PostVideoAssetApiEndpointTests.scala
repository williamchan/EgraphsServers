package controllers.api

import java.util.Date
import java.io.File
import org.apache.commons.io.FileUtils
import org.joda.time.DateTimeConstants

import play.api.http.HeaderNames
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

import controllers.routes.ApiControllers.postVideoAsset
import services.blobs.Blobs
import services.db._
import services.http.BasicAuth
import services.AppConfig
import utils.EgraphsUnitTest
import utils.TestData
import models.VideoAssetCelebrityStore

class PostVideoAssetApiEndpointTests
  extends EgraphsUnitTest
//FIXME: Play 2.1 migration broke these tests.  Seems to be an issue with multipart for data,
// and the Helper which had a lot of changes.
//  with ProtectedCelebrityResourceTests
{
//  protected override def routeUnderTest = postVideoAsset
//  protected override def validRequestBodyAndQueryString: Option[FakeRequest[_]] = {
//    Some(FakeRequest(routeUnderTest.method, routeUnderTest.url))
//  }
//  protected override def routeRequest(request: FakeRequest[_]): Option[Result] = {
//    routeAndCall(request)
//  }

  protected def db = AppConfig.instance[DBSession]

  ignore should "accept multipartFormData, respond with OK, and verify file creation in the blobstore" in new EgraphsTestApplication {

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
    val postBody = MultipartFormData[TemporaryFile](nonFiles, fakeVideoFile, Seq())

    val Some(result) = routeAndCall(FakeRequest(POST, controllers.routes.ApiControllers.postVideoAsset.url,
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))), postBody).withHeaders(auth.toHeader))

    status(result) should be(OK)

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
}