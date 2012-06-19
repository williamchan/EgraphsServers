package demosetup

import services.blobs.Blobs
import play.templates.JavaExtensions
import services.AppConfig
import services.db.Schema

case class DemoScenario(name: String, category: String = "Uncategorized", description: String="", instructions: () => Any) {
  /** Slug used to identify the demoScenario in the URL */
  def urlSlug: String = {
    JavaExtensions.slugify(name, false)
  }

  def play() = {
    try {
      instructions()
    }
    catch {
      case exc:Throwable =>
        throw new RuntimeException("Error while executing demoScenario \"" + name + "\"", exc)
    }
  }
}

object DemoScenario {
  /**
   * Lazy evaluator for the Demo Scenarios library, which should live with the test
   * code.
   *
   * Doing it this way ensures that we don't waste any memory loading demo scenarios
   * until the first demoScenario request comes in, which should only be in a test
   * environment.
   */
  lazy val demoScenarios = Class.forName("demosetup.DemoScenarios").newInstance()

  /** All registered demo scenarios, indexed by name */
  var all = Map[String, DemoScenario]()

  /** All registered demo scenarios, indexed by category */
  var allCategories = Map[String, List[DemoScenario]]()

  /** List of all registered demo scenarios in no particular order */
  def list: Iterable[DemoScenario] = {
    all.map { case (name, demoScenario) => demoScenario }
  }

  /** Returns a map of all registered demo scenarios, indexed by category */
  def categories: Map[String,  List[DemoScenario]] = {
    allCategories
  }

  /** Returns a demoScenario with the provided name */
  def withSlug(urlSlug: String): Option[DemoScenario] = {
    all.get(urlSlug)
  }

  /** All registered demoScenario names */
  def allNames: Iterable[String] = {
    for ((key, _) <- all) yield key
  }

  /** Adds a demo demoScenario to the list of registered demo scenarios */
  def add(demoScenario: DemoScenario) {
    all += (demoScenario.urlSlug -> demoScenario)

    allCategories += (demoScenario.category -> (demoScenario :: allCategories.get(demoScenario.category).getOrElse(List.empty[DemoScenario])))
  }

  /**
   * Clears all demo scenarios by scrubbing the database and blobstore. For god's sake don't
   * call this in production.
   */
  def clearAll() {
    AppConfig.instance[Schema].scrub()
    AppConfig.instance[Blobs].scrub()
  }

  /**
   * Play through one or more demo scenarios without processing their return values.
   */
  def play(names: String*) {
    demoScenarios

    for (name <- names) {
      DemoScenario.withSlug(name) match {
        case None =>
          throw new IllegalArgumentException("No demo demoScenario named \"" + name + "\" found.")

        case Some(demoScenario) =>
          demoScenario.play()
      }
    }
  }
}
