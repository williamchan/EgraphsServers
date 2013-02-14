package controllers

import scala.concurrent._
import scala.concurrent.duration._
import play.api.Play
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Input
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import utils.TestHelpers
import utils.FunctionalTestUtils._
import controllers.routes.WebsiteControllers.getBlob

class BlobControllersTests extends EgraphsUnitTest with NonProductionEndpointTests {
  override def routeUnderTest: Call = getBlob("a/b/derp.jpg")
  
  override def successfulRequest = {
    TestHelpers.putPublicImageOnBlobStore()
    
    FakeRequest(routeUnderTest.method, routeUnderTest.url)
  }
  
  routeName(routeUnderTest) should "make transmit the blob's data" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest(GET, getBlob("a/b/derp.jpg").url))
    
    status(result) should be (OK)
    
    result match {
      case result =>
        val actualFile = Play.getFile("test/resources/derp.jpg")
        val chunkedResult = result.asInstanceOf[ChunkedResult[Array[Byte]]]
        chunkedContentLength(chunkedResult) should be (actualFile.length)
    }
    
  }
  
  it should "404 if the blob was not found" in new EgraphsTestApplication {
    val Some(result) = route(FakeRequest(GET, getBlob("b/aioehroauher.jpg").url))
    
    status(result) should be (NOT_FOUND)
  }
  
  def chunkedContentLength(chunkedResult: ChunkedResult[Array[Byte]]): Int =  {
    var numBytes = 0
    val countIteratee = Iteratee.fold[Array[Byte], Unit](0) { (_, bytes) => numBytes += bytes.size }
    val futureIteratee = chunkedResult.chunks(countIteratee).asInstanceOf[Future[Iteratee[Array[Byte], Unit]]]
    
    val future = Await.result(futureIteratee, 5 seconds).run
    Await.result(future, 5 seconds)

    numBytes
  }
}