package controllers.website.nonproduction

import play.api.mvc.Results.{Ok, Forbidden, NotFound, InternalServerError}
import play.api.mvc.Action
import services.AppConfig
import services.http.ControllerMethod
import services.logging.Logging
import play.api.mvc.Controller
import services.http.filters.HttpFilters
import models.AccountStore
import utils.TestData

object TestControllers extends Controller with Logging {
  val controllerMethod = AppConfig.instance[ControllerMethod]
  val accountStore = AppConfig.instance[AccountStore]
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
          case _ => throw new Exception("blargh")
        }
      }
    }
  }
  
  def getTestAccountDetails(customerId: Long) = filters.requireApplicationId.test {
    controllerMethod() {
      Action {
        val maybeAccount = accountStore.findByCustomerId(customerId) 
        maybeAccount match {
          case None => InternalServerError("There is no account associated with the given customer ID")
          case Some(account) => Ok("Username and Password: " + account.email + " : " + TestData.defaultPassword)
        }
      }
    }
  }
}
