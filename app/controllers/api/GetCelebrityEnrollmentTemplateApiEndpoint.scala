package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import services.http.CelebrityAccountRequestFilters

private[controllers] trait GetCelebrityEnrollmentTemplateApiEndpoint {
  this: Controller =>
  protected def celebFilters: CelebrityAccountRequestFilters

  /**
   * Provides a single Celebrity's JSON representation for consumption by the API.
   *
   */
  def getCelebrityEnrollmentTemplate = {
    celebFilters.requireCelebrityAccount {
      (account, celebrity) => {

        val phonymsPhrases: List[String] = List(
          "Stop each car if it's little",
          "Play in the street up ahead",
          "A fifth wheel caught speeding",
          "It's been about two years since Davey kept shotguns",
          "Charlie did you think to measure the tree",
          "Tina got cued to make a quicker escape",
          "Joe books very few judges",
          "Here I was in Miami and Illinois"
        )
        val myNameIs: List[String] = List("My name is " + celebrity.publicName.getOrElse("..."))
        val _1_thru_5: List[String] = List("One, two, three, four, five")
        val _6_thru_10: List[String] = List("Six, seven, eight, nine, ten")
        val enrollmentPhrases: List[String] = myNameIs ::: _1_thru_5 ::: phonymsPhrases ::: phonymsPhrases ::: _6_thru_10 ::: myNameIs

        val enrollmentTemplate = Map("enrollmentPhrases" -> enrollmentPhrases)
        Serializer.SJSON.toJSON(enrollmentTemplate)
      }
    }
  }
}