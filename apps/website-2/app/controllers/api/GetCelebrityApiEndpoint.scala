package controllers.api

import play.api.mvc._
import sjson.json.Serializer
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}

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
        Ok(Serializer.SJSON.toJSON(celebrity.renderedForApi))
      }
    }
  }
}