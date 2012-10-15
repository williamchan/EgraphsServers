package controllers.website.admin

import play.api.mvc.Action
import play.api.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{AccountStore, CelebrityStore}
import play.mvc.results.NotFound
import controllers.website.consumer.CelebrityLandingConsumerEndpoint
import models.frontend.header.HeaderData
import models.frontend.footer.FooterData
import controllers.WebsiteControllers
import play.mvc.Scope.Session

private[controllers] trait GetCelebrityAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getCelebrityAdmin(celebrityId: Long, action: Option[String] = None) = Action { controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>

      (accountStore.findByCelebrityId(celebrityId), celebrityStore.findById(celebrityId)) match {
        case (Some(account), Some(celebrity)) =>
          action match {
            case Some("preview") => {
              CelebrityLandingConsumerEndpoint.getCelebrityLandingHtml(celebrity)(HeaderData(), FooterData(), Session.current())
            }

            case _ => {
              val flash = play.mvc.Http.Context.current().flash()
              flash.put("celebrityId", celebrity.id)
              flash.put("celebrityEmail", account.email)
              flash.put("bio", celebrity.bio)
              flash.put("casualName", celebrity.casualName.getOrElse(""))
              flash.put("organization", celebrity.organization)
              flash.put("roleDescription", celebrity.roleDescription)
              flash.put("twitterUsername", celebrity.twitterUsername.getOrElse(""))
              flash.put("publicName", celebrity.publicName)
              flash.put("publishedStatusString", celebrity.publishedStatus.toString)

              GetCelebrityDetail.getCelebrityDetail(isCreate = false, celebrity = Some(celebrity))
            }
          }

        case _ => new NotFound("No such celebrity")
      }
    }
  }}
}

object GetCelebrityAdminEndpoint {

  def url(celebrityId: Long, action: Option[String] = None) = {
    controllers.routes.WebsiteControllers.getCelebrityAdmin(celebrityId, action).url
  }
}
