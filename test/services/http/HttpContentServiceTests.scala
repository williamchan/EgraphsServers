package services.http

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import play.mvc.Scope
import utils.EgraphsUnitTest

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
}