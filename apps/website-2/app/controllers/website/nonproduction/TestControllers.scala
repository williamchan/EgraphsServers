package controllers.website.nonproduction

import play.api.mvc.Results.{Ok, Forbidden, NotFound, InternalServerError}
import play.api.mvc.Action
import services.AppConfig
import services.http.ControllerMethod
import services.logging.Logging
import play.api.mvc.Controller
import services.http.filters.HttpFilters

object TestControllers extends Controller with Logging {
  val controllerMethod = AppConfig.instance[ControllerMethod]
  private val filters = AppConfig.instance[HttpFilters]

  def logStuffThenThrowException() = filters.requireApplicationId.test {
    controllerMethod() {
      Action {
        log("I'm pretty happy to be alive")
        log("Just chugging along with this method")
        log("Can't wait to send a nice webpage down to the client...")
        log("Wait, is that a bear???")
        log("Oh no! It's a bear!")
        log("Don't kill me please...I don't, I don't want to--")

        val illegalE = new IllegalArgumentException("Bear")
        throw new RuntimeException("Process was mauled by a bear", illegalE)

        Ok
      }
    }
  }

  def throwError(error: String) = filters.requireApplicationId.test {
    controllerMethod() {
      Action {
        error match {
          case "403" => Forbidden("")
          case "404" => NotFound("")
          case _ => InternalServerError
        }
      }
    }
  }
}
