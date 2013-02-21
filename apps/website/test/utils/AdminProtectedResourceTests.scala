package utils

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils._
import controllers.routes.WebsiteControllers.getLoginAdmin
import services.db.{DBSession, TransactionSerializable}
import models._
import play.api.libs.Files.TemporaryFile
import play.mvc.Http.HeaderNames
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.Date
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{MultipartFormData, Call}

abstract trait AdminProtectedResourceTestBase { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  protected def db : DBSession

  def admin : Administrator = {
    db.connected(TransactionSerializable) {TestData.newSavedAdministrator()}
  }
}

trait AdminProtectedResourceTests extends AdminProtectedResourceTestBase { this: EgraphsUnitTest =>
  routeName(routeUnderTest) + ", as an admin authenticated resource, " should "fail due to lack of an admin id in the session" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest().toCall(routeUnderTest).withAuthToken)
    status(result) should be (SEE_OTHER)
    headers(result)("Location") should be (getLoginAdmin.url)
  }
  
  it should "not redirect to the login page session" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest().toCall(routeUnderTest).withAdmin(admin.id).withAuthToken)
    redirectLocation(result) should not be (Some(getLoginAdmin.url))
  }
}

trait AdminProtectedMultipartFormResourceTests extends AdminProtectedResourceTestBase { this: EgraphsUnitTest =>

  routeName(routeUnderTest) + ", as an admin authenticated resource, " should "fail due to lack of an admin id in the session" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(request.toRoute(routeUnderTest).withAuthToken)
    status(result) should be (SEE_OTHER)
    headers(result)("Location") should be (getLoginAdmin.url)
  }

  it should "not redirect to the login page session" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(request.toRoute(routeUnderTest).withAdmin(admin.id).withAuthToken)
    redirectLocation(result) should not be (Some(getLoginAdmin.url))
  }

  private def request = {
    val tempFile = File.createTempFile("afakefile", "txt")
    FileUtils.writeStringToFile(tempFile, (new Date()).toString)
    val filename = "test.txt"
    val fakeImageFile = Seq(FilePart("testFile", filename, Some("text/plain"),
      play.api.libs.Files.TemporaryFile(tempFile)))
    val body = MultipartFormData[TemporaryFile](
      Map("example" -> Seq("example")),
      fakeImageFile,
      badParts = Seq(),
      missingFileParts = Seq()
    )
    
    FakeRequest().copy(
      POST,
      routeUnderTest.url,
      FakeHeaders(Map(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))),
      body
    )
  }
}

