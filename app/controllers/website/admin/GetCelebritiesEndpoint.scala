package controllers.website.admin

import models.{Celebrity, Account, CelebrityStore}
import org.squeryl.Query
import play.mvc.Controller

private[controllers] trait GetCelebritiesEndpoint {
  this: Controller =>

  protected def celebrityStore: CelebrityStore

  def getCelebrities = {
    val celebrityAccounts: Query[(Celebrity, Account)] = celebrityStore.getCelebrityAccounts
    views.Application.html.admin_celebrities(celebrityAccounts = celebrityAccounts)
  }
}
