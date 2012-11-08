// TODO: PLAY20 MIGRATION: myyk - I think that we will mostly delete this file (or wholly) since this doesn't seem to be how you hook into 
//   Play2 in tests.
package utils

import play.api.test.FakeRequest
import play.api.mvc.{AnyContent, Call}
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
      "Deliver-All-Orders-to-Celebrities"
    )
  }
  
  /** 
   * Returns the contents of a ChunkedResult[Array[Byte]] as a vector of bytes. Throws
   * an exception otherwise.
   **/
  def chunkedContent(result: Result): IndexedSeq[Byte] =  {
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
  
  trait NonProductionEndpointTests { this: EgraphsUnitTest =>
    import play.api.test.Helpers._
    protected def routeUnderTest: Call
    protected def successfulRequest: FakeRequest[AnyContent] = {
      FakeRequest(routeUnderTest.method, routeUnderTest.url)
    }
    
    private def nonTestApplication: FakeApplication = {
      val normalTestConfig = EgraphsUnitTest.testApp.configuration  
      val configWithNonTestAppId = normalTestConfig ++ Configuration.from(Map("application.id" -> "not-test"))
      new FakeApplication(path=EgraphsUnitTest.testApp.path) {
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
  
  
  class DomainRequest[T <: AnyContent](request: FakeRequest[T]) {
    def toRoute(route: Call): FakeRequest[T] = {
      request.copy(method=route.method, uri=route.url)
    }
    
    def withCustomer(customerId: Long): FakeRequest[T] = {
      request.withSession(request.session.withCustomerId(customerId).data.toSeq: _*)
    }
    
    def withAdmin(adminId: Long): FakeRequest[T] = {
      request.withSession(request.session.withAdminId(adminId).data.toSeq: _*)
    }
    
    def withAuthToken: FakeRequest[AnyContentAsFormUrlEncoded] = {
      val authToken = "fake-auth-token"
      val existingBody = request.body.asFormUrlEncoded.getOrElse(Map())
      val newBody = existingBody + ("authenticityToken" -> Seq(authToken))
      val newBodySingleValues = newBody.map(kv => (kv._1, kv._2.head))
      val newSession = request.session + ("authenticityToken" -> authToken)
      request
        .withSession(newSession.data.toSeq:_*)
        .withFormUrlEncodedBody(newBodySingleValues.toSeq: _*)
    }
  }
  
  object Conversions {
    implicit def fakeRequestToDomainRequest[T <: AnyContent](fakeRequest: FakeRequest[T]) = {
      new DomainRequest(fakeRequest)
    }
  }
}