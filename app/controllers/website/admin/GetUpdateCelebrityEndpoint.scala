package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{AccountStore, CelebrityStore}

private[controllers] trait GetUpdateCelebrityEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getUpdateCelebrity(celebrityId: Long) = controllerMethod() {
    adminFilters.requireAdministratorLogin { adminId =>
      val account = accountStore.findByCelebrityId(celebrityId).get
      val celebrity = celebrityStore.findById(celebrityId).get
      flash.put("celebrityId", celebrity.id)
      flash.put("celebrityEmail", account.email)
      flash.put("firstName", celebrity.firstName.get)
      flash.put("lastName", celebrity.lastName.get)
      flash.put("publicName", celebrity.publicName.get)
      flash.put("description", celebrity.description.get)

      GetCelebrityDetail.getCelebrityDetail(isCreate = false)
    }
  }
}

object GetUpdateCelebrityEndpoint {

  def url(celebrityId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getUpdateCelebrity", Map("celebrityId" -> celebrityId.toString))
  }
}