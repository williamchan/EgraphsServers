package controllers.website.nonproduction

import play.api.mvc.Controller
import models.{AccountStore, Account}
import services.AppConfig
import services.http.ControllerMethod
import play.api.mvc.Action
import play.api.mvc.Results.Ok
import services.http.filters.HttpFilters

/**
 * Test controller used in [[controllers.DBTransactionTests]] to verify that Play does, in fact,
 * open a transaction and rollback/commit it correctly for each request.
 */
object TransactionTestController extends Controller {
  private val controllerMethod = AppConfig.instance[ControllerMethod]
  private val filters = AppConfig.instance[HttpFilters]

  def makeAccountAndThrowException() = filters.requireApplicationId.test {
    controllerMethod() {
      Action {
        createSavedAccount()
        throw new RuntimeException(
          "Throwing this should cause the account not to persist"
        )
        
        Ok
      }
    }
  }

  def isStored = filters.requireApplicationId.test {
    controllerMethod() {
      Action {
        Ok(AppConfig.instance[AccountStore].findByEmail("erem@egraphs.com").headOption match {
          case None => "Nope"
          case _ => "Yep"
        })
      }
    }
  }

  def makeAccount() = filters.requireApplicationId.test {
    controllerMethod() {
      Action {
        createSavedAccount()
        
        Ok
      }
    }
  }

  //
  // Private members
  //
  private def createSavedAccount():Account = {
    Account(email="erem@egraphs.com").save()
  }
}