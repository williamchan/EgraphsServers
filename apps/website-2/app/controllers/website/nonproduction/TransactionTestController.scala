package controllers.website.nonproduction

import play.api.mvc.Controller
import models.{AccountStore, Account}
import services.AppConfig
import services.http.ControllerMethod
import play.api.mvc.Action
import play.api.mvc.Results.Ok

/**
 * Test controller used in [[controllers.DBTransactionTests]] to verify that Play does, in fact,
 * open a transaction and rollback/commit it correctly for each request.
 */
object TransactionTestController extends Controller {
  private val controllerMethod = AppConfig.instance[ControllerMethod]

  def makeAccountAndThrowException() = controllerMethod() {
    Action {
      createSavedAccount()
      throw new RuntimeException(
        "Throwing this should cause the account not to persist"
      )
      
      Ok
    }
  }

  def isStored = controllerMethod() {
    Action {
      Ok(AppConfig.instance[AccountStore].findByEmail("erem@egraphs.com").headOption match {
        case None => "Nope"
        case _ => "Yep"
      })
    }
  }

  def makeAccount() = controllerMethod() {
    Action {
      createSavedAccount()
      
      Ok
    }
  }

  private def createSavedAccount():Account = {
    Account(email="erem@egraphs.com").save()
  }

}