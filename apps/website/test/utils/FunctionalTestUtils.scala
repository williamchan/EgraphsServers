package utils

import scala.concurrent._
import scala.concurrent.duration._
import play.api.test.FakeRequest
import play.api.mvc.{ AnyContent, AnyContentAsEmpty, Call }
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
import play.api.test._
import play.api.http.HeaderNames
import models._
import scenario.RepeatableScenarios

/**
 * Common functionality required when writing functional tests against
 * controller methods.
 */
object FunctionalTestUtils {

  implicit class EgraphsFakeRequest[A](request: FakeRequest[A]) {
    def toCall(call: Call): FakeRequest[A] = {
      // make a new FakeRequest using the old one since the copy doesn't work exactly
      // as we would like it to since it would return a play.api.mvc.RequestHeader
      new FakeRequest(
        method = call.method,
        uri = call.url,
        headers = request.headers,
        body = request.body,
        remoteAddress = request.remoteAddress,
        version = request.version,
        id = request.id,
        tags = request.tags
      )
    }

    def withCustomerId(id: Long): FakeRequest[A] = {
      request.withSession(EgraphsSession.Key.CustomerId.name -> id.toString)
    }

    def withAdminId(id: Long): FakeRequest[A] = {
      request.withSession(EgraphsSession.Key.AdminId.name -> id.toString)
    }

    def withCredentials(user: String, password: String): FakeRequest[A] = {
      val auth = BasicAuth.Credentials(user, password)
      request.withHeaders(auth.toHeader)
    }

    def withCredentials(account: Account, password: String = TestData.defaultPassword): FakeRequest[A] = {
      withCredentials(account.email, password)
    }
  }

  /**
   * This method is designed to be a more thread safe version of runWillChanScenariosThroughOrder()
   */
  def runCustomerBuysProductsScenerio(): (Customer, Celebrity, Iterable[Product], Iterable[Order]) = {
    val celebrity = RepeatableScenarios.createCelebrity(isFeatured = true)
    val products = RepeatableScenarios.celebrityHasProducts(celebrity, numberOfProducts = 2)
    val customer = TestData.newSavedCustomer()
    val unapprovedOrders = 
      RepeatableScenarios.customerBuysEveryProductOfCelebrity(customer, celebrity) ++ RepeatableScenarios.customerBuysEveryProductOfCelebrity(customer, celebrity)
    val orders = RepeatableScenarios.deliverOrdersToCelebrity(unapprovedOrders)

    (customer, celebrity, products.toList, orders.toList)
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
        val futureIteratee = chunkedByteResult.chunks(countIteratee).asInstanceOf[Future[Iteratee[Array[Byte], Unit]]]

        val future = Await.result(futureIteratee, 5 seconds).run
        Await.result(future, 5 seconds)

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
    protected def routeUnderTest: Call
    protected def successfulRequest: FakeRequest[AnyContentAsEmpty.type] = {
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
      val Some(result) = route(successfulRequest)
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

  /**
   * Typical use case for including an authenticity token in a post request.
   * (Overriden method requestWithAuthTokenInBody used by withAuthToken in trait DomainRequestBase:
   *   type checks for anything that can be cast as AnyContentAsFormUrlEncoded)
   */
  implicit class DomainRequest[T <: AnyContent](override val request: FakeRequest[T]) extends DomainRequestBase[T] {
    override def requestWithAuthTokenInBody: FakeRequest[T] = {
      val formUrlEncodedRequest = request.asInstanceOf[FakeRequest[AnyContentAsFormUrlEncoded]]
      val existingBody = formUrlEncodedRequest.body.asFormUrlEncoded.getOrElse(Map())
      val newBody = existingBody + ("authenticityToken" -> Seq(authToken))
      val newBodySingleValues = newBody.map(kv => (kv._1, kv._2.head))

      request.withFormUrlEncodedBody(newBodySingleValues.toSeq: _*).asInstanceOf[FakeRequest[T]]
    }
  }

  /**
   * Multipart use case for including an authenticity token in a post request.
   * (Overriden method requestWithAuthTokenInBody used by withAuthToken in trait DomainRequestBase:
   *   type checks for MultipartFormData[TemporaryFile])
   */
  implicit class MultipartDomainRequest[T](override val request: FakeRequest[T]) extends DomainRequestBase[T] {
    override def requestWithAuthTokenInBody: FakeRequest[T] = {

      val multipartEncodedRequest = request.asInstanceOf[FakeRequest[MultipartFormData[TemporaryFile]]]

      val existingDataParts = multipartEncodedRequest.body.dataParts
      val existingFiles = multipartEncodedRequest.body.files
      val existingBadParts = multipartEncodedRequest.body.badParts
      //TODO: verify, this seems to be removed in Play 2.1 this might be okay
      //val existingMissingFileParts = multipartEncodedRequest.body.missingFileParts

      val newDataParts = existingDataParts + ("authenticityToken" -> Seq(authToken))

      val newMultipart = MultipartFormData[TemporaryFile](newDataParts, existingFiles, existingBadParts)
      val newRequest = FakeRequest(request.method, request.uri, request.headers, newMultipart)
      newRequest.asInstanceOf[FakeRequest[T]]
    }
  }
}