package controllers.api

import models.Celebrity
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import sjson.json.Serializer

private[controllers] trait GetCelebrityEnrollmentTemplateApiEndpoint { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  
  /**
   * Provides a single Celebrity's JSON representation for consumption by the API.
   *
   */
  def getCelebrityEnrollmentTemplate = controllerMethod() {
    httpFilters.requireAuthenticatedAccount() { account =>
      httpFilters.requireCelebrityId.inAccount(account) { celeb =>
        Action {
          getCelebrityEnrollmentTemplateResult(celeb)
        }
      }
    }
  }
  
  private def getCelebrityEnrollmentTemplateResult(celebrity: Celebrity): Result = {
    // IMPORTANT: The total number of enrollment phrases must match EnrollmentBatch.batchSize
    val phonymsPhrases: List[String] = List(
      "Stop each car if it's little",
      "Play in the street up ahead",
      "A fifth wheel caught speeding",
      "It's been about two years since Davey kept shotguns",
      "Charlie did you think to measure the tree",
      "Tina got cued to make a quicker escape",
      "Joe books very few judges",
      "Here I was in Miami and Illinois")
    val myNameIs: List[String] = List("My name is " + celebrity.publicName)
    val _1_thru_5: List[String] = List("One, two, three, four, five")
    val _6_thru_10: List[String] = List("Six, seven, eight, nine, ten")
    val enrollmentPhrases: List[String] = myNameIs ::: _1_thru_5 ::: phonymsPhrases ::: phonymsPhrases ::: _6_thru_10 ::: myNameIs
    val enrollmentPhrasesWithFieldIdentifiers: List[Map[String, String]] = for (enrollmentPhrase <- enrollmentPhrases) yield Map(GetCelebrityEnrollmentTemplateApiEndpoint._text -> enrollmentPhrase)

    val enrollmentTemplate = Map(GetCelebrityEnrollmentTemplateApiEndpoint._enrollmentPhrases -> enrollmentPhrasesWithFieldIdentifiers)
    Ok(Serializer.SJSON.toJSON(enrollmentTemplate))
  }
}

object GetCelebrityEnrollmentTemplateApiEndpoint {
  val _enrollmentPhrases: String = "enrollmentPhrases"
  val _text: String = "text"
}
