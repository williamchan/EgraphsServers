package demosetup

trait DeclaresDemoScenarios {
  /** Entry point to the scenario declaration language */
  val toDemoScenarios = DemoScenarioDeclaration

  object DemoScenarioDeclaration {
    def add (demoScenario: DemoScenario) {
      DemoScenario.add(demoScenario)
    }
  }
}
