package controllers

import play.api.Play
import play.api.mvc.Request
import play.api.mvc.Controller
import play.api.test.Helpers._
import play.api.test.FakeRequest
import _root_.utils.EgraphsUnitTest
import _root_.utils.TestHelpers
import _root_.utils.FunctionalTestUtils.{routeName, NonProductionEndpointTests}
import controllers.routes.WebsiteControllers.getBlob
import play.api.mvc.ChunkedResult
import play.api.libs.iteratee.Iteratee
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.Input
import play.api.mvc.ChunkedResult
import play.api.mvc.Call
import play.api.test.FakeRequest$

class BlobControllersTests extends EgraphsUnitTest with NonProductionEndpointTests {
  override def routeUnderTest: Call = getBlob("a/b/derp.jpg")
  
  override def successfulRequest = {
    TestHelpers.putPublicImageOnBlobStore()
    
    FakeRequest(routeUnderTest.method, routeUnderTest.url)
  }
  
  routeName(routeUnderTest) should "make transmit the blob's data" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest(GET, getBlob("a/b/derp.jpg").url))
    
    status(result) should be (OK)
    
    result match {
      case chunkedResult: ChunkedResult[Array[Byte]] =>
        val actualFile = Play.getFile("test/resources/derp.jpg")
        chunkedContentLength(chunkedResult) should be (actualFile.length)
        
      case otherType =>
        fail("Expected ChunkedResult instead of " + otherType.getClass)
    }
    
  }
  
  it should "404 if the blob was not found" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest(GET, getBlob("b/aioehroauher.jpg").url))
    
    status(result) should be (NOT_FOUND)
  }
  
  def chunkedContentLength(chunkedResult: ChunkedResult[Array[Byte]]): Int =  {
    var numBytes = 0
    val countIteratee = Iteratee.fold[Array[Byte], Unit](0) { (_, bytes) => numBytes += bytes.size }
    val promisedIteratee = chunkedResult.chunks(countIteratee).asInstanceOf[Promise[Iteratee[Array[Byte], Unit]]]
    
    promisedIteratee.await(5000).get.run.await(5000).get

    numBytes
  }
}