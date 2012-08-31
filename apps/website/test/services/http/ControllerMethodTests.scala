package services.http

import play.mvc.results.Forbidden
import utils.{MockControllerMethod, EgraphsUnitTest}
import play.mvc.Scope.Session
import com.google.inject.util.Modules
import services.AppConfig
import com.google.inject.{Guice, AbstractModule}
import uk.me.lings.scalaguice.{InjectorExtensions, ScalaModule}
import InjectorExtensions._

class ControllerMethodTests extends EgraphsUnitTest {
  "POSTControllerMethod and POSTApiControllerMethod" should "correctly specify whether csrfToken authentication should occur" in {
    implicit var (request, session) = newRequestAndMockSession

    // Temporarily pretend we're on live for this test (this enables csrf checks in general)
    val postController = fakeInjector(playId="live").instance[POSTControllerMethod]
    val postApiController = fakeInjector(playId="live").instance[POSTApiControllerMethod]

    // Test: operation value should return if authenticity token is same as parameter
    request.params.put("authenticityToken", "herp")
    session.getAuthenticityToken.returns("herp")

    (postController() { "=)" }) should be ("=)")
    (postApiController { "=)" }) should be ("=)")

    // Test: Forbidden should be returned if authenticity token is different as parameter
    // The POSTApiController should still work though
    session = mock[Session]
    session.getAuthenticityToken.returns("derp")

    (postController() { ">=(" }).asInstanceOf[AnyRef].getClass should be (classOf[Forbidden])
    (postApiController { "=)" }) should be ("=)")
  }

  def instanceWithDeps = {
    val controllerMethod = MockControllerMethod
    val authenticityTokenFilterProvider = mock[RequireAuthenticityTokenFilterProvider]

    (new POSTControllerMethod(controllerMethod, authenticityTokenFilterProvider),
     controllerMethod,
     authenticityTokenFilterProvider)
  }

  private def fakeInjector(playId: String) = {
    object FakeAuthenticityTokenModule extends AbstractModule with ScalaModule {
      override def configure() {
        bind[String].annotatedWith[PlayId].toInstance(playId)
      }
    }

    val testModule = Modules.`override`(new AppConfig()).`with`(FakeAuthenticityTokenModule)
    Guice.createInjector(testModule)
  }
}