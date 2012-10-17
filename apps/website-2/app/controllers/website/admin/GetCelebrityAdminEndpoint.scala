package controllers.website.admin

import play.api.mvc.Controller
import models.{AccountStore, CelebrityStore}
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect, NotFound}
import controllers.website.consumer.CelebrityLandingConsumerEndpoint
import models.frontend.header.HeaderData
import models.frontend.footer.FooterData

private[controllers] trait GetCelebrityAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getCelebrityAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        (accountStore.findByCelebrityId(celebrityId), celebrityStore.findById(celebrityId)) match {
          case (Some(account), Some(celebrity)) =>
            // TODO(play2): I had a hard time getting url parameter and query parameters working together. Will figure out later.
            if (request.queryString.get("action").getOrElse("").toString.contains("preview")) {
              implicit val headerData = HeaderData()
              implicit val footerData = FooterData()
              Ok(CelebrityLandingConsumerEndpoint.getCelebrityLandingHtml(celebrity))
            } else {
              implicit val flash = request.flash + 
              ("celebrityId" -> celebrity.id.toString) + 
              ("celebrityEmail" -> account.email) + 
              ("bio" -> celebrity.bio) + 
              ("casualName" -> celebrity.casualName.getOrElse("")) + 
              ("organization" -> celebrity.organization) + 
              ("roleDescription" -> celebrity.roleDescription) + 
              ("twitterUsername" -> celebrity.twitterUsername.getOrElse("")) + 
              ("publicName" -> celebrity.publicName) + 
              ("publishedStatusString" -> celebrity.publishedStatus.toString)
              GetCelebrityDetail.getCelebrityDetail(isCreate = false, celebrity = Some(celebrity))
            }
          case _ => NotFound("No such celebrity")
        }
      }
    }
  }
}

object GetCelebrityAdminEndpoint {

  def url(celebrityId: Long) = {
    controllers.routes.WebsiteControllers.getCelebrityAdmin(celebrityId).url
  }
}
