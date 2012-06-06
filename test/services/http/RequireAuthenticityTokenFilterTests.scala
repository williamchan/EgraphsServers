package services.http

import utils.EgraphsUnitTest
import play.test.FunctionalTest
import play.mvc.Scope.Session
import play.mvc.Http.Request

class RequireAuthenticityTokenFilterTests extends EgraphsUnitTest {
  val doCheckClass = classOf[DoRequireAuthenticityToken]
  val dontCheckClass = classOf[DontRequireAuthenticityToken]

  "RequireAuthenticityTokenFilterProvider" should "provide Guice the correct filter implementations in test and non-test modes" in {
    val testModeClass = new RequireAuthenticityTokenFilterProvider("test").get.getClass
    testModeClass should be (classOf[DontRequireAuthenticityToken])

    // It should always provide the real checker in deployed configurations.
    for (deployedPlayId <- List("live", "demo", "staging")) {
      val deployModeClass = new RequireAuthenticityTokenFilterProvider(deployedPlayId).get.getClass
      deployModeClass should be (classOf[DoRequireAuthenticityToken])
    }
  }

  "RequireAuthenticityTokenFilterProvider.apply" should "never return the real implementation during test mode" in {
    for (checkValue <- List(true, false)) {
      getFilterViaProviderApply("test", checkValue).getClass should be (dontCheckClass)
    }
  }

  "RequireAuthenticityTokenFilterProvider.apply" should "return the real implementation during production modes based on doCheck" in {
    getFilterViaProviderApply("live", doCheck=true).getClass should be (doCheckClass)
    getFilterViaProviderApply("live", doCheck=false).getClass should be (dontCheckClass)
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

  def getFilterViaProviderApply(playId: String, doCheck: Boolean): RequireAuthenticityTokenFilter = {
    new RequireAuthenticityTokenFilterProvider(playId).apply(doCheck)
  }
}
