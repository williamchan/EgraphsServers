package services.http

import utils.EgraphsUnitTest
import models.EnrollmentSample
import play.mvc.Http.Response

class HttpContentServiceTests extends EgraphsUnitTest {

  def underTest = new HttpContentService

  "HttpContentService" should "return the correct SVGZ content" in {
    val headers = underTest.headersForFilename("a/b/herp.svgz")
    headers.contentType should be ("image/svg+xml")
    headers.contentEncoding should be (Some("gzip"))
  }

  it should "otherwise delegate to Play implementations" in {
    val headers = underTest.headersForFilename("a/b/herp.png")
    headers.contentType should be ("image/png")
    headers.contentEncoding should be (None)
  }

  it should "get the correct type for text files even in the absence of a Response" in {
    val origResponse = Response.current()
    try {
      Response.current.set(null)
      val headers = underTest.headersForFilename(EnrollmentSample.getSignatureXmlUrl(0))
      headers.contentType should include ("text/xml")
    }
    finally {
      Response.current.set(origResponse)
    }
  }
}