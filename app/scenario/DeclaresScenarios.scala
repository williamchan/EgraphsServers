package scenario

/**
 * Trait whose extending classes can use a nice DSL to register new
 * scenarios.
 *
 * Usage:
 * <code>
 *   class Scenarios extends DeclaresScenarios {
 *     scenario named "Shaq is a celebrity" is {
 *       // Add a Celebrity object to the database for Shaquille O'Neal
 *     }
 *
 *     scenario named "Shaq has 2 products" is {
 *       // Add 2 products to Shaq's database
 *     }
 *   }
 * </code>
 */
trait DeclaresScenarios {
  /** Entry point to the scenario declaration language */
  val scenario = ScenarioDeclaration

  object ScenarioDeclaration {
    def named (nameDeclaration: ScenarioNameDeclaration) = nameDeclaration
  }

  class ScenarioNameDeclaration(name: String) {
    def is (function: => Unit) {
      val scenario = Scenario(name, () => function)
      Scenario.add(scenario)
    }
  }

  implicit def convertStringToScenarioNameDeclaration(name: String): ScenarioNameDeclaration = {
    new ScenarioNameDeclaration(name)
  }
}



