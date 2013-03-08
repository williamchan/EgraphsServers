package controllers.api

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import models.JsCelebrity
import models.JsCelebrityContactInfo
import models.JsCelebrityDepositInfo

private[controllers] trait GetCelebrityApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  /**
   * Provides a single Celebrity's JSON representation for consumption by the API.
   *
   * See [[https://egraphs.atlassian.net/wiki/pages/viewpage.action?pageId=12943525]].
   */
  def getCelebrity = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCelebrityId.inAccount(account) { celeb =>
        Action {
          Ok(Json.toJson(JsCelebrity.from(celeb)))
        }
      }
    }
  }

  /**
   * Provides a single Celebrity's contact info JSON representation for consumption by the API.
   *
   * See [[https://egraphs.atlassian.net/wiki/pages/viewpage.action?pageId=21889081]].
   */
  def getCelebrityContactInfo = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCelebrityId.inAccount(account) { celeb =>
        Action {
          Ok(Json.toJson(JsCelebrityContactInfo.from(celeb)))
        }
      }
    }
  }

  /**
   * Provides a single Celebrity's deposit info JSON representation for consumption by the API.
   *
   * See [[https://egraphs.atlassian.net/wiki/pages/viewpage.action?pageId=21889081]].
   */
  def getCelebrityDepositInfo = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCelebrityId.inAccount(account) { celeb =>
        Action {
          Ok(Json.toJson(JsCelebrityDepositInfo.from(celeb)))
        }
      }
    }
  }
}