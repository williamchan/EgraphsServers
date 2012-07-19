package controllers.website

import play.test.FunctionalTest
import uk.me.lings.scalaguice.ScalaModule
import services.http.{RequireAuthenticityTokenFilter, RequireAuthenticityTokenFilterProvider}
import play.mvc.Scope.Session
import play.mvc.Http.Request
import play.mvc.results.Forbidden
import com.google.inject.AbstractModule
import utils.{ClearsDatabaseAndValidationBefore, TestWebsiteControllers, TestAppConfig, EgraphsUnitTest}

class PostSecurityTests extends EgraphsUnitTest with ClearsDatabaseAndValidationBefore {
  "Website POST controllers" should "require authenticity token checks" in {
    val (endpoints, forbiddenInstance) = websiteControllersThatAlwaysFailAuthenticityCheck
    implicit val request = FunctionalTest.newRequest()

    val endpointInvocationsThatRequireTokens = List(
      () => endpoints.postLoginAdmin(null, null),
      () => endpoints.postLogout(),
      () => endpoints.postAccountAdmin(0, null, null),
      () => endpoints.postCelebrityAdmin(0, null, null, null, null, null, null, null, null, null),
      () => endpoints.postCelebrityProductAdmin(0, null, null, null, null, 0, 0, null, null, null),
      () => endpoints.postOrderAdmin(0),
      () => endpoints.postEgraphAdmin(0),
      () => endpoints.postCelebrityInventoryBatchAdmin(0, 0, null, null),
      () => endpoints.postResetPassword()
    )

    for (endpointInvocation <- endpointInvocationsThatRequireTokens) {
      endpointInvocation() match {
        case forbidden: Forbidden =>
          forbidden.getMessage should be (forbiddenInstance.getMessage)

        case somethingElse =>
          fail("Expected a Forbidden but instead got: " + somethingElse)
      }
    }
  }

  private def websiteControllersThatAlwaysFailAuthenticityCheck: (AllWebsiteEndpoints, Forbidden) = {
    // Slightly modify our application config to always fail authenticity checks and return
    // an implementation of our endpoints tied to that configuration (along with the specific
    // instance of Forbidden that should be returned
    val forbiddenMessage = "failed authenticity token check"
    val forbidden = new Forbidden(forbiddenMessage)

    val fakeAuthenticityCheckProvider = new RequireAuthenticityTokenFilterProvider(null) {
      override def apply(doCheck: Boolean): RequireAuthenticityTokenFilter = {
        new RequireAuthenticityTokenFilter {
          override def apply[A](operation: => A)(implicit session: Session, request: Request): Either[Forbidden, A] = {
            Left(forbidden)
          }
        }
      }
    }

    val moduleWithAlwaysFailAuthenticityCheck = new AbstractModule with ScalaModule {
      def configure() {
        bind[RequireAuthenticityTokenFilterProvider].toInstance(fakeAuthenticityCheckProvider)
      }
    }

    val testEndpoints = new TestAppConfig(moduleWithAlwaysFailAuthenticityCheck).instance[TestWebsiteControllers]

    (testEndpoints, forbidden)
  }
}
