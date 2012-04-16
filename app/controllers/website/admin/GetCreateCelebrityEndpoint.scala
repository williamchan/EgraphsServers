package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{AccountStore, CelebrityStore}

private[controllers] trait GetCreateCelebrityEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getCreateCelebrity = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      GetCelebrityDetail.getCelebrityDetail(isCreate = true)
    }
  }
}

object GetCreateCelebrityEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getCreateCelebrity")
  }
}