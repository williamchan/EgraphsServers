package controllers

import play.test.Fixtures
import play.mvc.Controller
import scenario.{Scenario}
import play.mvc.results.{Result, ScalaAction}
import play.Play

/**
 * Controller for all scenarios
 */
object ScenarioController extends Controller {
  /**
   * Lazy evaluator for the Scenarios library, which should live with the test
   * code.
   *
   * Doing it this way ensures that we don't waste any memory loading scenarios
   * until the first scenario request comes in, which should only be in a test
   * environment.
   */
  lazy val scenarios = Class.forName("Scenarios").newInstance()

  /**
   * Performs a code block only if the project's Scenarios library is available.
   *
   * @returns the result of the code block if the project's Scenarios library
   *   is available. 500 (Internal Error) and an informative response if not
   */
  private def withRegisteredScenarios(task: => Any) = {
    try {
      scenarios

      task
    } catch {
      case e: ClassNotFoundException =>
        this.Error(
          """
          No scenarios available. Ensure a class named Scenarios that
          extends DeclaresScenarios is on the classpath. See documentation on
          trait DeclaresScenarios for more information.
          """
        )
    }
  }

  /**
   * Clears the entire database, and with it all scenarios. As such, for God's
   * sake ensure that you never hook this up to the production database.
   *
   * @return 200 (Ok) if successful.
   */
  def clear = withRegisteredScenarios {
    Fixtures.deleteDatabase()

    "All scenarios cleared."
   }


  /**
   * Returns a human-readable list of the available scenarios.
   *
   * @return 200 (Ok) and the list if successful.
   */
  def list = withRegisteredScenarios {
    val scenarioNames = Scenario.allNames.toSeq
    <html><body>
      { scenarioNames.length } scenarios:
      <ul>
        { Scenario.allNames.map(name => <li><code>{ name }</code></li>) }
      </ul>
    </body></html>
  }

  /**
   * Executes a named scenario.
   *
   * @return 200 (Ok) and a useful human-readable message if successful.
   */
  def scenario (name: String) = withRegisteredScenarios {
    Scenario.named(name) match {
      case Some(existingScenario) => {
        existingScenario.play()
        Html("Scenario <pre>" + name + "</pre> successfully replayed.")
      }
      case _ => {
        NotFound(
          "No scenario was found with the name \"" + name + "\"."+
          "View available scenarios at " + reverse(this.list)
        )
      }
    }
  }
}
