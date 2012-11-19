package controllers.website.admin

import utils.EgraphsUnitTest
import services.AppConfig
import utils.AdminProtectedResourceTests
import utils.TestData
import models.VideoAsset
import services.db.{ DBSession, TransactionSerializable, Schema }
import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import models.VideoAssetStore
import models.enums.VideoStatus
import utils.FunctionalTestUtils.Conversions._
import utils.FunctionalTestUtils
import utils.FunctionalTestUtils.MultipartDomainRequest
import models.VideoAsset
import play.api.http.HeaderNames
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import controllers.routes.WebsiteControllers.postVideoAssetAdmin
import java.io.File
import services.blobs.Blobs

class PostVideoAssetAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  protected override def routeUnderTest = postVideoAssetAdmin
  protected def db = AppConfig.instance[DBSession]

  it should "accept multipartFormData, respond with OK, and verify file creation in the blobstore" in new EgraphsTestApplication {

    val (admin, celebrity) = db.connected(TransactionSerializable) {
      val admin = TestData.newSavedAdministrator()
      val celebrity = TestData.newSavedCelebrity()
      (admin, celebrity)
    }

    val tempFile = File.createTempFile("videoFile", "mp4")

    val filename = "test.mp4"
    val nonFiles = Map("celebrityId" -> Seq(celebrity.id.toString))
    val fakeVideoPart = FilePart("video", filename, Some("video/mp4"),
      play.api.libs.Files.TemporaryFile(tempFile))

    val fakeVideoFile = Seq(fakeVideoPart)
    val postBody = MultipartFormData[TemporaryFile](nonFiles, fakeVideoFile, Seq(), Seq())

    val Some(result) = routeAndCall(new MultipartDomainRequest(FakeRequest(POST, "/admin/videoasset",
      FakeHeaders(Map(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))), postBody)).withAdmin(admin.id).withAuthToken)     

    status(result) should be(OK)

    val blob: Blobs = AppConfig.instance[Blobs]
    val videoKey = "videos/" + celebrity.id + "/" + filename
    val maybeFileLocation = blob.getUrlOption(key = videoKey)
    
    maybeFileLocation.isDefined should be(true)    
  }
}