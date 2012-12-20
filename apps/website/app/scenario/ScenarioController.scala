package controllers.website.nonproduction

import play.api.mvc.Controller
import scenario.Scenario
import play.api.mvc.Result
import services.http.{ControllerMethod, WithDBConnection}
import services.AppConfig
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.templates.Html
import services.http.filters.HttpFilters

/**
 * Controller for all scenarios
 */
object ScenarioController extends Controller {
  protected val controllerMethod = AppConfig.instance[ControllerMethod]
  protected val filters = AppConfig.instance[HttpFilters]  

  /**
   * Performs a code block only if the project's Scenarios library is available.
   *
   * @return the result of the code block if the project's Scenarios library
   *   is available. 500 (Internal Error) and an informative response if not
   */
  private def withRegisteredScenarios[A](action: Action[A])
  : Action[A] = {
    Action(action.parser) { request =>
      try {
        Scenario.scenarios

        action(request)
      } catch {
        case e: ClassNotFoundException =>
          InternalServerError(
            """
            No scenarios available. Ensure a class named Scenarios that
            extends DeclaresScenarios is on the classpath. See documentation on
            trait DeclaresScenarios for more information.
            """)
      }
    }
  }

  /**
   * Clears the entire database, and with it all scenarios. As such, for God's
   * sake ensure that you never hook this up to the production database.
   *
   * @return 200 (Ok) if successful.
   */
  def clear = filters.requireApplicationId.test {
    controllerMethod(dbSettings = WithDBConnection(readOnly = false)) {
      withRegisteredScenarios {
        Action {
          Scenario.clearAll()
          Ok("All scenarios cleared.")
        }
      }
    }
  }

  /**
   * Returns a human-readable list of the available scenarios.
   *
   * @return 200 (Ok) and the list if successful.
   */
  def list = filters.requireApplicationId.test {
    controllerMethod() {
      withRegisteredScenarios {
        Action {
          val scenarios = Scenario.allCategories.toList.sortWith((a, b) => a._1 < b._1).map { case (category, catScenarios) =>
            (category, catScenarios.toSeq.sortWith((a, b) => a.name < b.name))
          }
          Ok(views.html.nonproduction.scenarios(scenarios))
        }
      }
    }
  }

  /**
   * Executes a named scenario.
   *
   * @return 200 (Ok) and a useful human-readable message if successful.
   */
  def scenario (urlSlug: String) = filters.requireApplicationId.test { 
    controllerMethod(dbSettings = WithDBConnection(readOnly = false)) {
      withRegisteredScenarios {
        Action {
          Scenario.withSlug(urlSlug) match {
            case Some(existingScenario) => {
              existingScenario.play() match {
                case aResult: Result =>
                  aResult
                case instructionResponse =>
                  Ok(views.html.nonproduction.scenario_success(urlSlug, existingScenario.description, instructionResponse))
              }
            }
    
            case None => {
              NotFound(
                "No scenario was found with the name \"" + urlSlug + "\"."+
                  "View available scenarios at " + controllers.website.nonproduction.routes.ScenarioController.list().url
              )
            }
          }
        }
      }
    }
  }
}
