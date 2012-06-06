package services.http

import utils.EgraphsUnitTest
import services.AppConfig.instance
import play.test.FunctionalTest
import play.mvc.Scope.Session
import play.mvc.Http.Request

class RequireAuthenticityTokenFilterTests extends EgraphsUnitTest {
  "RequireAuthenticityTokenFilterProvider" should "provide the correct filter implementations in test and non-test modes" in {
    filterInstanceForPlayId("test").getClass should be (classOf[DontRequireAuthenticityToken])

    // It should always provide the real checker in deployed configurations.
    for (deployedPlayId <- List("live", "demo", "staging")) {
      filterInstanceForPlayId(deployedPlayId).getClass should be (classOf[DoRequireAuthenticityToken])
    }
  }

  "Both filter implementations" should "behave correctly when authenticity tokens don't match" in {
    // Setup
    implicit val (request, session) = newRequestAndMockSession(
      requestToken="something",
      sessionToken="something else"
    )

    val List(dontRequireAuthenticity, doRequireAuthenticity) = makeBothFilterImplementations

    // Tests
    dontRequireAuthenticity { "=D" } should be (Right("=D"))
    doRequireAuthenticity { "=(" }.isLeft should be (true)
  }

  "Both filter implementations" should "behave correctly when authenticity tokens match" in {
    // Setup
    implicit val (request, session) = newRequestAndMockSession(
      requestToken="matchthis",
      sessionToken="matchthis"
    )

    // Execute test on both filters
    for (filterImplementation <- makeBothFilterImplementations) {
      filterImplementation { "=D" } should be (Right("=D"))
    }
  }

  private def filterInstanceForPlayId(playId: String): RequireAuthenticityTokenFilter = {
    new RequireAuthenticityTokenFilterProvider(playId).get
  }

  private def newRequestAndMockSession(requestToken: String, sessionToken: String): (Request, Session) = {
    val request = FunctionalTest.newRequest()
    val session = mock[play.mvc.Scope.Session]

    request.params.put("authenticityToken", requestToken)
    session.getAuthenticityToken.returns(sessionToken)

    (request, session)
  }

  private def makeBothFilterImplementations: List[RequireAuthenticityTokenFilter] = {
    List(new DontRequireAuthenticityToken, new DoRequireAuthenticityToken)
  }
}
