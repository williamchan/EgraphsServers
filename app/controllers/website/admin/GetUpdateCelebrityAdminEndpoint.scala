package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{AccountStore, CelebrityStore}

private[controllers] trait GetUpdateCelebrityAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getUpdateCelebrityAdmin(celebrityId: Long) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val account = accountStore.findByCelebrityId(celebrityId).get
      val celebrityOption = celebrityStore.findById(celebrityId)
      val celebrity = celebrityOption.get
      flash.put("celebrityId", celebrity.id)
      flash.put("celebrityEmail", account.email)
      flash.put("firstName", celebrity.firstName.get)
      flash.put("lastName", celebrity.lastName.get)
      flash.put("publicName", celebrity.publicName.get)
      flash.put("description", celebrity.description.get)

      GetCelebrityDetail.getCelebrityDetail(isCreate = false, celebrity = celebrityOption)
    }
  }
}

object GetUpdateCelebrityAdminEndpoint {

  def url(celebrityId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getUpdateCelebrityAdmin", Map("celebrityId" -> celebrityId.toString))
  }
}