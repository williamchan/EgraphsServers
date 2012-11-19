// TODO: PLAY20 MIGRATION: myyk - I think that we will mostly delete this file (or wholly) since this doesn't seem to be how you hook into 
//   Play2 in tests.
package utils

import play.api.test.FakeRequest
import play.api.mvc.{ AnyContent, Call }
import play.api.Play
import play.api.test.Helpers._
import play.api.Configuration
import services.http.BasicAuth
import play.api.test.FakeApplication
import services.http.EgraphsSession
import EgraphsSession.Conversions._
import play.api.mvc.Call
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.ChunkedResult
import play.api.libs.iteratee.Iteratee
import play.api.libs.concurrent.Promise
import play.api.mvc.Result
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile

//import java.util.Properties
//import models.Account
//import play.api.mvc.Http.Request
//import play.api.test.FunctionalTest
//
/**
 * Common functionality required when writing functional tests against
 * controller methods.
 */
object FunctionalTestUtils {
  //  /**
  //   * Makes an account identified by wchan83@egraphs.com/derp
  //   */
  //  def willChanAccount: Account = {
  //    Account(email = "wchan83@egraphs.com").withPassword(TestData.defaultPassword).right.get
  //  }
  //
  //  /**
  //   * Makes an API request verified by the credentials from `willChanAccount`
  //   */
  def willChanRequest: FakeRequest[AnyContent] = {
    val auth = BasicAuth.Credentials("wchan83@egraphs.com", TestData.defaultPassword)

    FakeRequest().withHeaders(auth.toHeader)
  }

  def requestWithCustomerId(id: Long): FakeRequest[AnyContent] = {
    FakeRequest().withSession(EgraphsSession.Key.CustomerId.name -> id.toString)
  }

  def requestWithAdminId(id: Long): FakeRequest[AnyContent] = {
    FakeRequest().withSession(EgraphsSession.Key.AdminId.name -> id.toString)
  }

  def requestWithCredentials(user: String, password: String): FakeRequest[AnyContent] = {
    val auth = BasicAuth.Credentials(user, password)

    FakeRequest().withHeaders(auth.toHeader)
  }
  //
  //  def createRequest(host: String = "www.egraphs.com", url: String = "/", secure: Boolean = false): Request = {
  //    val request = FunctionalTest.newRequest()
  //    request.host = host
  //    request.url = url
  //    request.secure = secure
  //    request
  //  }
  //
  //  def createProperties(propName: String, propValue: String): Properties = {
  //    val playConfig = new Properties
  //    playConfig.setProperty(propName, propValue)
  //    playConfig
  //  }
  //
  def runScenarios(names: String*) {
    names.foreach { name =>
      runScenario(name)
    }
  }

  def runFreshScenarios(names: String*) {
    runScenario("clear")
    runScenarios(names: _*)
  }

  def runScenario(name: String) {
    val result = routeAndCall(FakeRequest(GET, "/test/scenarios/" + name)).get
    if (status(result) != OK) {
      throw new IllegalArgumentException("Unknown scenario name " + name)
    }
  }

  def runWillChanScenariosThroughOrder() {
    runFreshScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each",
      "Deliver-All-Orders-to-Celebrities")
  }

  /**
   * Returns the contents of a ChunkedResult[Array[Byte]] as a vector of bytes. Throws
   * an exception otherwise.
   */
  def chunkedContent(result: Result): IndexedSeq[Byte] = {
    result match {
      case chunkedResult: ChunkedResult[_] =>
        val chunkedByteResult = chunkedResult.asInstanceOf[ChunkedResult[Array[Byte]]]
        var bytesVec = Vector.empty[Byte]
        val countIteratee = Iteratee.fold[Array[Byte], Unit](0) { (_, bytes) => bytesVec = bytesVec ++ bytes }
        val promisedIteratee = chunkedByteResult.chunks(countIteratee).asInstanceOf[Promise[Iteratee[Array[Byte], Unit]]]

        promisedIteratee.await(5000).get.run.await(5000).get

        bytesVec

      case _ =>
        throw new Exception("Couldn't get chunked content from result of type " + result.getClass)
    }
  }

  def routeName(call: Call): String = {
    call.method + " " + call.url
  }

  def extractId(location: String): Long = {
    location.substring(location.lastIndexOf("/") + 1).toLong
  }

  trait NonProductionEndpointTests { this: EgraphsUnitTest =>
    import play.api.test.Helpers._
    protected def routeUnderTest: Call
    protected def successfulRequest: FakeRequest[AnyContent] = {
      FakeRequest(routeUnderTest.method, routeUnderTest.url)
    }

    private def nonTestApplication: FakeApplication = {
      val normalTestConfig = EgraphsUnitTest.testApp.configuration
      val configWithNonTestAppId = normalTestConfig ++ Configuration.from(Map("application.id" -> "not-test"))
      new FakeApplication(path = EgraphsUnitTest.testApp.path) {
        override def configuration = configWithNonTestAppId
      }
    }

    routeName(routeUnderTest) + ", as a test-only endpoint, " should "be available during test mode" in new EgraphsTestApplication {
      val Some(result) = routeAndCall(successfulRequest)
      status(result) should not be (NOT_FOUND)
    }

    // TODO: Implement this method once controllers are injectable...Until then it will be impossible
    // to configure the app with a different ConfigFileProxy.
    // of our app will inhibit being able to create a controller with a different ConfigFileProxy
    it should "be unavailable outside of test mode" in (pending)
  }

  trait DomainRequestBase[T] {
    def request: FakeRequest[T]
    def requestWithAuthTokenInBody: FakeRequest[T]

    val authToken = "fake-auth-token"

    def toRoute(route: Call): FakeRequest[T] = {
      request.copy(method = route.method, uri = route.url)
    }

    def withCustomer(customerId: Long): FakeRequest[T] = {
      request.withSession(request.session.withCustomerId(customerId).data.toSeq: _*)
    }

    def withAdmin(adminId: Long): FakeRequest[T] = {
      request.withSession(request.session.withAdminId(adminId).data.toSeq: _*)
    }

    def withAuthToken: FakeRequest[T] = {
      val newSession = request.session + ("authenticityToken" -> authToken)
      requestWithAuthTokenInBody.withSession(newSession.data.toSeq: _*)
    }

  }

  class DomainRequest[T <: AnyContent](override val request: FakeRequest[T]) extends DomainRequestBase[T] {
    override def requestWithAuthTokenInBody: FakeRequest[T] = {
      val formUrlEncodedRequest = request.asInstanceOf[FakeRequest[AnyContentAsFormUrlEncoded]]
      val existingBody = formUrlEncodedRequest.body.asFormUrlEncoded.getOrElse(Map())
      val newBody = existingBody + ("authenticityToken" -> Seq(authToken))
      val newBodySingleValues = newBody.map(kv => (kv._1, kv._2.head))

      request.withFormUrlEncodedBody(newBodySingleValues.toSeq: _*).asInstanceOf[FakeRequest[T]]
    }
  }

  class MultipartDomainRequest[T](override val request: FakeRequest[T]) extends DomainRequestBase[T] {
    override def requestWithAuthTokenInBody: FakeRequest[T] = {
      var multipartEncodedRequest = request.asInstanceOf[FakeRequest[MultipartFormData[TemporaryFile]]]
      val existingDataParts = multipartEncodedRequest.body.dataParts
      val existingFiles = multipartEncodedRequest.body.files
      val newDataParts = existingDataParts + ("authenticityToken" -> Seq(authToken))
      //val newDataPartsSingleValues = newDataParts.map(kv => (kv._1, kv._2.head))
      

      //request.asInstanceOf[FakeRequest[MultipartFormData[T]]].body.dataParts = newDataParts 
      //request.asInstanceOf[FakeRequest[T]]
      //request.withFormUrlEncodedBody(newDataPartsSingleValues.toSeq: _*).asInstanceOf[FakeRequest[T]]

      //val putBody = MultipartFormData[TemporaryFile](newDataParts, Seq(), Seq(), Seq())
// if (maybeVideosAndPublicNames.exists { case (_, maybePublicName) => maybePublicName.isEmpty })
            //request.map(FakeRequest(dataParts => newDataParts))
      println("request body looks like: " + request.body)
      println("***********")
      
      // make a new MultipartFormData guy
      // copy old contents 
      // add addition
      // return whole thing?
      
      //request.body
//      Foo(a,b, c)
//      someFoo match {
//        case Foo(x, y, _) => new Foo(x, y, bar)
//      }
      
     val newRequest = MultipartFormData[TemporaryFile](newDataParts, existingFiles, Seq(), Seq())
      
      //val newMultipart = MultipartFormData[TemporaryFile]
      
      //(request.map { case part => newDataParts }).asInstanceOf[FakeRequest[T]]
      
     
     newRequest.asInstanceOf[FakeRequest[T]]
     
    }
  }

  object Conversions {
    implicit def fakeAnyContentRequestToDomainRequest[T <: AnyContent](fakeRequest: FakeRequest[T]) = {
      new DomainRequest(fakeRequest)
    }

    implicit def fakeMultipartContentRequestToDomainRequest[T](fakeRequest: FakeRequest[T]) = {
      new MultipartDomainRequest(fakeRequest)
    }
  }
}