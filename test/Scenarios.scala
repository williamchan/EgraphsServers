import models.{Account, Celebrity}
import scenario.DeclaresScenarios

/**
 * All scenarios supported by the API.
 */
class Scenarios extends DeclaresScenarios {
  scenario named "Will-Chan-is-a-celebrity" is {
    val celebrity = Celebrity(firstName=Some("William"), lastName=Some("Chan"), popularName=Some("Wizzle")).save()
    Account(email="wchan83@gmail.com", celebrityId=Some(celebrity.id)).withPassword("herp").right.get.save
  }

  scenario named "Will-has-two-products" is {
    // Implement the scenario here
  }
}
