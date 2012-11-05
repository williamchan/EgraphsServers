package services.http.filters

import com.google.inject.Inject
import services.config.ConfigFileProxy
import play.api.mvc.Action
import play.api.mvc.Results.NotFound
import services.http.DeploymentTarget
import play.api.mvc.Result
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse

/**
 * Filters out requests based on the application.id value in the active config file.
 * Usage:
 * {{{
 *   trait MyTestOnlyEndpoint { this: Controller =>
 *     def httpFilters: HttpFilters
 *
 *     def doThisInTestOnly = httpFilters.requireApplicationId.test {
 *       Action {
 *         // Your code in here
 *       }
 *     }
 *
 *     def doThisInStagingOnly = httpFilters.requireApplicationId("staging")
 *   }
 * }}}
 */
class RequireApplicationId @Inject() (config: ConfigFileProxy) extends Filter[String, Unit] {
  override def filter(requiredAppId: String): Either[Result, Unit] = {
    if (config.applicationId == requiredAppId) Right()
    else Left(NotFound)
  }

  def test[A](action: => Action[A], parser: BodyParser[A] = parse.anyContent): Action[A] = {
    this.apply(DeploymentTarget.Test, parser)(_ => action)
  }
}