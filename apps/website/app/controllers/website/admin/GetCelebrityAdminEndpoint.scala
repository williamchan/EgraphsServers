package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{AccountStore, CelebrityStore}
import controllers.website.GetCelebrityEndpoint
import play.mvc.results.NotFound

private[controllers] trait GetCelebrityAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getCelebrityAdmin(celebrityId: Long, action: Option[String] = None) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>

      (accountStore.findByCelebrityId(celebrityId), celebrityStore.findById(celebrityId)) match {
        case (Some(account), Some(celebrity)) =>
          action match {
            case Some("preview") => {
              GetCelebrityEndpoint.html(celebrity)
            }

            case _ => {
              flash.put("celebrityId", celebrity.id)
              flash.put("celebrityEmail", account.email)
              flash.put("bio", celebrity.bio)
              flash.put("casualName", celebrity.casualName.getOrElse(""))
              flash.put("organization", celebrity.organization)
              flash.put("roleDescription", celebrity.roleDescription.getOrElse(""))
              flash.put("twitterUsername", celebrity.twitterUsername.getOrElse(""))
              flash.put("firstName", celebrity.firstName.getOrElse(""))
              flash.put("lastName", celebrity.lastName.getOrElse(""))
              flash.put("publicName", celebrity.publicName.getOrElse(""))
              flash.put("description", celebrity.description.getOrElse(""))
              flash.put("publishedStatusString", celebrity.publishedStatus.toString)

              GetCelebrityDetail.getCelebrityDetail(isCreate = false, celebrity = Some(celebrity))
            }
          }

        case _ => new NotFound("No such celebrity")
      }
    }
  }
}

object GetCelebrityAdminEndpoint {

  def url(celebrityId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getCelebrityAdmin", Map("celebrityId" -> celebrityId.toString))
  }
}