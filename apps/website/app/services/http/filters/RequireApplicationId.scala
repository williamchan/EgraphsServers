package services.http.filters

import com.google.inject.Inject
import services.config.ConfigFileProxy
import play.api.mvc.Action
import play.api.mvc.Results.NotFound
import services.http.DeploymentTarget

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
 * 
 * TODO: PLAY20 migration. test this.
 */
class RequireApplicationId @Inject()(config: ConfigFileProxy) {
  def apply[A](requiredAppId: String)(action: Action[A]): Action[A] = {
    Action(action.parser) { request =>
      if (config.applicationId == requiredAppId) action(request) else NotFound
    }
  }

  def test[A](action: Action[A]): Action[A] = {
    this.apply(DeploymentTarget.Test)(action)
  }
}