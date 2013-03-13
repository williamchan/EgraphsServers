package controllers.website.admin

import utils.{TestData, EgraphsUnitTest, AdminProtectedMultipartFormResourceTests, CsrfProtectedMultipartFormResourceTests}
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import models.enums.PublishedStatus
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.Date
import utils.FunctionalTestUtils.Conversions._
import play.api.mvc.{Result, MultipartFormData}
import play.api.libs.Files.TemporaryFile
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.mvc.MultipartFormData.FilePart
import scala.Some
import play.mvc.Http.HeaderNames
import egraphs.playutils.Gender
import models.CelebrityStore

trait PostCelebrityAdminEndpointTests
{
  protected def db = AppConfig.instance[DBSession]

  protected def createBody(
    email: String,
    password: String = TestData.generateUsername(),
    publicName: String = TestData.generateFullname(),
    publishedStatusString: String  = PublishedStatus.Published.name,
    bio: String = "A man, a plan, a canal, Panama!",
    casualName: String = "",
    gender: String = Gender.Neutral.name,
    organization: String = "wonderful team",
    roleDescription: String = "wonderful person",
    twitterUsername: String = TestData.generateUsername()
  ): MultipartFormData[TemporaryFile] = {

    val tempFile = File.createTempFile("afakefile", "txt")
    FileUtils.writeStringToFile(tempFile, (new Date()).toString)

    val fakeImageFile = Seq(FilePart("testFile", "text.txt", Some("text/plain"),
      play.api.libs.Files.TemporaryFile(tempFile)))

    MultipartFormData[TemporaryFile](
      Map(
        "celebrityEmail" -> Seq(email),
        "celebrityPassword" -> Seq(password),
        "publicName" -> Seq(publicName),
        "publishedStatusString" -> Seq(publishedStatusString),
        "bio" -> Seq(bio),
        "casualName" -> Seq(casualName),
        "gender" -> Seq(gender),
        "organization" -> Seq(organization),
        "roleDescription" -> Seq(roleDescription),
        "twitterUsername" -> Seq(twitterUsername),
        "facebookUrl" -> Seq(""),
        "websiteUrl" -> Seq("")
      ),
      fakeImageFile,
      badParts = Seq())
  }
}

class PostCreateCelebrityAdminEndpointTests extends PostCelebrityAdminEndpointTests
  with EgraphsUnitTest
  with CsrfProtectedMultipartFormResourceTests
  with AdminProtectedMultipartFormResourceTests
{
  protected def controllerMethod = controllers.WebsiteControllers.postCreateCelebrityAdmin
  protected def routeUnderTest = controllers.routes.WebsiteControllers.postCreateCelebrityAdmin
  def celebrityStore = AppConfig.instance[CelebrityStore]

  routeUnderTest.url should "create a celeb" in new EgraphsTestApplication {
    db.connected(TransactionSerializable) {
      val body = createBody(email = TestData.generateEmail())
      val Some(result) = performRequest(body, adminId = admin.id)
      status(result) should be(SEE_OTHER)
      redirectLocation(result).getOrElse("").contains("?action=preview") should be(true)
    }
  }

  it should "not allow you to create an account with a duplicate email address" in new EgraphsTestApplication {
    val (celebrity, account) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account)
    }

    val body = createBody(email = account.email)

    db.connected(TransactionSerializable) {
      val Some(result) = performRequest(body, adminId = admin.id)
        status(result) should be(SEE_OTHER)
        redirectLocation(result) should be(Some(controllers.routes.WebsiteControllers.getCreateCelebrityAdmin().url))
    }
  }

  it should "not allow you to create an account with an email address in use by a customer account" in new EgraphsTestApplication {
    val (customer, account) = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      (customer, customer.account)
    }

    val body = createBody(email = account.email)

    db.connected(TransactionSerializable) {
      val Some(result) = performRequest(body, adminId = admin.id)
      status(result) should be(SEE_OTHER)
      redirectLocation(result) should be(Some(controllers.routes.WebsiteControllers.getCreateCelebrityAdmin().url))
    }
  }

  it should "not allow you to create an account with an email address in use by an admin account" in new EgraphsTestApplication {
    val (admin, account) = db.connected(TransactionSerializable) {
      val admin = TestData.newSavedAdministrator()
      (admin, admin.account)
    }

    val body = createBody(email = account.email)

    db.connected(TransactionSerializable) {
      val Some(result) = performRequest(body, adminId = admin.id)
      status(result) should be(SEE_OTHER)
      redirectLocation(result) should be(Some(controllers.routes.WebsiteControllers.getCreateCelebrityAdmin().url))
    }
  }

  private def performRequest(body: MultipartFormData[TemporaryFile], adminId: Long) : Option[Result] = {
    Some(controllers.WebsiteControllers.postCreateCelebrityAdmin(
      FakeRequest(POST,
        routeUnderTest.url,
        FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data"))),
        body = body
      ).withAuthToken.withAdmin(adminId)))
  }
}
