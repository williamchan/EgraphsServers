package scenario

/**
 * Trait whose extending classes can use a nice DSL to register new
 * scenarios.
 *
 * Usage:
 * <code>
 *   class Scenarios extends DeclaresScenarios {
 *     toScenarios add Scenario(
 *       "Shaq is a celebrity",
 *
 *       """
 *       Creates a Celebrity with name Shaquille 'Shaq' O'Neal. His login/password are
 *       shaq@herp.com/attack
 *       """,
 *
 *       {() =>
 *          // Implement the code that realizes the scenario.
 *          // You can return an action as if this were a Controller
 *          // and running the scenario will perform the action (e.g. a Redirect)
 *       }
 *     )
 *   }
 * </code>
 */
trait DeclaresScenarios {
  /** Entry point to the scenario declaration language */
  val toScenarios = ScenarioDeclaration

  object ScenarioDeclaration {
    def add (scenario: Scenario) {
      Scenario.add(scenario)
    }
  }
}



