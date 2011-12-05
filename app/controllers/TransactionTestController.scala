package controllers

import play.mvc.Controller
import models.Account

/**
 * Test controller used in [[controllers.DBTransactionTests]] to verify that Play does, in fact,
 * open a transaction and rollback/commit it correctly for each request.
 */
object TransactionTestController extends Controller with DBTransaction {

  def makeAccountAndThrowException() {
    makeAccount()
    throw new RuntimeException(
      "Throwing this should cause the account not to persist"
    )
  }

  def isStored = {
    Account.findByEmail("erem@egraphs.com").headOption match {
      case None => "Nope"
      case _ => "Yep"
    }
  }

  def makeAccount() {
    Account(email="erem@egraphs.com").save()
    Ok
  }

}