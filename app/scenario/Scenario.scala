package scenario

import libs.Blobs

/**
 * An executable scenario.
 *
 * A scenario is a testing tool that associates a name with
 * the eGraphs Database being in a certain state. It should
 * be exposed to the outside only when running in tests mode.
 *
 * For example, you may write a scenario named "Shaq is a celebrity" that
 * creates a Celebrity user named Shaquille O'Neal. You may have another one
 * "Shaq has a product" that adds a Product to his collection.
 *
 * For how to register scenarios, create a class named Scenarios in the
 * tests directory, and register scenarios in it.
 *
 * For more, see [[scenarios.DeclaresScenarios]]
 */
case class Scenario(name: String, description: String="", play: () => Any)

object Scenario {
  /** All registered scenarios, indexed by name */
  var all: Map[String, Scenario] = Map[String, Scenario]()

  /** List of all registered scenarios in no particular order */
  def list: Iterable[Scenario] = {
    all.map {case (name, scenario) => scenario }
  }

  /** Returns a scenario with the provided name */
  def named(name: String): Option[Scenario] = {
    all.get(name)
  }

  /** All registered scenario names */
  def allNames: Iterable[String] = {
    for ((key, _) <- all) yield key
  }

  /** Adds a scenario to the list of registered scenarios */
  def add(scenario: Scenario) {
    all += (scenario.name -> scenario)
  }

  /**
   * Clears all scenarios by scrubbing the database and blobstore. For god's sake don't
   * call this in production.
   */
  def clearAll() {
    db.Schema.scrub()
    Blobs.scrub()
  }

  /**
   * Play through one or more scenarios without processing their return values.
   */
  def play(names: String*) {
    for (name <- names) {
      Scenario.named(name) match {
        case None =>
          throw new IllegalArgumentException("No scenario named \"" + name + "\" found.")

        case Some(scenario) =>
          scenario.play()
      }
    }
  }
}


