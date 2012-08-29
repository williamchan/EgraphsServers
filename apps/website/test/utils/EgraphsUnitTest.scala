package utils

import play.test.{FunctionalTest, UnitFlatSpec}
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import play.mvc.Http.Request
import play.mvc.Scope.Session

/**
 * Convenience method provides most generally used traits for a scalatest
 */
trait EgraphsUnitTest extends UnitFlatSpec
  with ShouldMatchers
  with Mockito {

  def newRequestAndMockSession: (Request, Session) = {
    (FunctionalTest.newRequest(), mock[play.mvc.Scope.Session])
  }
}
