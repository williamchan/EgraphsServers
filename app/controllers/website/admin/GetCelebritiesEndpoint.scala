package controllers.website.admin

import models.{Celebrity, Account, CelebrityStore}
import org.squeryl.Query
import play.mvc.Controller
import services.http.ControllerMethod

private[controllers] trait GetCelebritiesEndpoint {
  this: Controller =>

  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod

  def getCelebrities = controllerMethod() {
    val celebrityAccounts: Query[(Celebrity, Account)] = celebrityStore.getCelebrityAccounts
    views.Application.admin.html.admin_celebrities(celebrityAccounts = celebrityAccounts)
  }
}
