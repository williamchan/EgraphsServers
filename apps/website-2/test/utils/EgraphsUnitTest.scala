package utils

import org.specs2.mock.Mockito
import play.mvc.Http.Request
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.mvc.Session
import play.api.test.FakeRequest

/**
 * Convenience method provides most generally used traits for a scalatest
 */
trait EgraphsUnitTest extends FlatSpec
  with ShouldMatchers
  with Mockito {

  def newRequestAndMockSession: (FakeRequest[_], Session) = {
    (FakeRequest(), mock[Session])
  }
}
