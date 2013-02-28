package services.mvc

import play.api.mvc.{MultipartFormData, Action}
import play.api.libs.Files.TemporaryFile
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.Date
import play.api.test.{FakeRequest, FakeHeaders}
import play.api.test.Helpers._
import play.api.mvc.MultipartFormData.FilePart
import scala.Some
import play.mvc.Http.HeaderNames
import play.api.mvc.Call

/**
 * Provides helpful methods for testing MultipartForm controllers.
 *
 * Example usage:
 * trait DoNothingTest extends MultipartFormTestHelper { this: EgraphsUnitTest =>
 *    protected def routeUnderTest = postMastheadAdmin
 *    protected def controllerMethod = controllers.WebsiteControllers.postMastheadAdmin
 *
 *  routeName(routeUnderTest) should "accept the multipart request" in new EgraphsTestApplication {
 *    val result = controllerMethod(request)
 *
 *    status(result) should not be (OK)
 * }
 *
 */

trait MultipartFormTestHelper {
  protected def controllerMethod: Action[MultipartFormData[TemporaryFile]]
  protected def routeUnderTest: Call
  protected def method: String = POST

  /*
   * Prepares a fake multipart form request to be routed via the controller method.
   */
  def request: FakeRequest[MultipartFormData[TemporaryFile]] = {
    val tempFile = File.createTempFile("afakefile", "txt")
    FileUtils.writeStringToFile(tempFile, (new Date()).toString)

    val body = MultipartFormData[TemporaryFile](
      Map("example" -> Seq("example")),
      Seq(FilePart("testFile", "test.txt", Some("text/plain"), play.api.libs.Files.TemporaryFile(tempFile))),
      badParts = Seq()
    )
    FakeRequest(
      POST,
      routeUnderTest.url,
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))),
      body
    )
  }
}
