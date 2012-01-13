package controllers

import play.mvc.Controller
import play.mvc.results.Result
import demosetup.DemoScenario

/**
 * Controller for all scenarios
 */
object DemoSetupController extends Controller with DBTransaction {
  /**
   * Performs a code block only if the project's Demo Scenarios library is available.
   *
   * @returns the result of the code block if the project's Demo Scenarios library
   *   is available. 500 (Internal Error) and an informative response if not
   */
  private def withRegisteredDemoScenarios(task: => Any) = {
    try {
      DemoScenario.demoScenarios

      task
    } catch {
      case e: ClassNotFoundException =>
        this.Error(
          """
          No demo scenarios available. Ensure a class named Demo Scenarios that
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
  def clear = withRegisteredDemoScenarios {
    DemoScenario.clearAll()
    "All demo scenarios cleared."
  }

  /**
   * Returns a human-readable list of the available demo scenarios.
   *
   * @return 200 (Ok) and the list if successful.
   */
  def list = withRegisteredDemoScenarios {
    val demoScenarios = DemoScenario.allCategories.toList.sortWith((a, b) => a._1 < b._1).map { case (category, catScenarios) =>
      (category, catScenarios.toSeq.sortWith((a, b) => a.name < b.name))
    }
    views.Application.html.demoscenarios(demoScenarios)
  }

  /**
   * Executes a named demoScenario.
   *
   * @return 200 (Ok) and a useful human-readable message if successful.
   */
  def demoScenario (urlSlug: String) = withRegisteredDemoScenarios {
    DemoScenario.withSlug(urlSlug) match {
      case Some(existingDemoScenario) => {
        existingDemoScenario.play() match {
          case aResult: Result =>
            aResult
          case _ =>
            Html(
              "Demo Scenario <pre>"
                + urlSlug
                + "</pre> successfully replayed.<br/><br/>"
                + existingDemoScenario.description
            )
        }
      }

      case None => {
        NotFound(
          "No demoScenario was found with the name \"" + urlSlug + "\"."+
            "View available demo scenarios at " + reverse(this.list)
        )
      }
    }
  }
}
