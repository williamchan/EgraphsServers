package controllers.api

import play.api.mvc._
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import models.Celebrity
import play.api.libs.json.JsValue
import services.Time

private[controllers] trait GetCelebrityApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  /**
   * Provides a single Celebrity's JSON representation for consumption by the API.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Celebrities%C2%A0Celebrities the json spec]].
   */
  def getCelebrity = Action { implicit request =>
    controllerMethod() {
      celebFilters.requireCelebrityAccount { (account, celebrity) =>
        Ok(celebrityToJson(celebrity))
      }
    }
  }

  /**
   * Renders the Celebrity as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  // maybe we need a toJson Helper object
  def celebrityToJson(celebrity: Celebrity): JsValue = {
    toJson(
      Map(
        "id" -> toJson(celebrity.id),
        "enrollmentStatus" -> toJson(celebrity.enrollmentStatus.name),
        "publicName" -> toJson(celebrity.publicName),
        "urlSlug" -> toJson(celebrity.urlSlug)
      ) ++
      // these could be for any HasCreatedUpdated object
      Map(
        "created" -> toJson(Time.toApiFormat(celebrity.created)),
        "updated" -> toJson(Time.toApiFormat(celebrity.updated))
      )
    )
  }
}
