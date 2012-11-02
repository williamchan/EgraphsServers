package controllers.website.admin

import play.api.mvc.Controller
import models.{AccountStore, CelebrityStore, Celebrity}
import models.enums.PublishedStatus
import models.filters._
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect, NotFound}
import controllers.website.consumer.CelebrityLandingConsumerEndpoint
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import org.apache.commons.lang3.StringEscapeUtils

private[controllers] trait GetCelebrityAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore
  protected def filterValueStore: FilterValueStore  

  def getCelebrityAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        (accountStore.findByCelebrityId(celebrityId), celebrityStore.findById(celebrityId)) match {
          case (Some(account), Some(celebrity)) =>
            // TODO(play2): I had a hard time getting url parameter and query parameters working together. Will figure out later.
            if (request.queryString.get("action").getOrElse("").toString.contains("preview")) {
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
              val (errorFields, fieldDefaults) = getCelebrityDetail(celebrity = Some(celebrity))
              val celebFilterValueIds = (for(filterValue <- celebrity.filterValues) yield { filterValue.id }).toSet

              Ok(views.html.Application.admin.admin_celebritydetail(
                isCreate = false,
                errorFields = errorFields,
                fields = fieldDefaults,
                celebrity = Option(celebrity),
                currentFilterValueIds = celebFilterValueIds,
                filterValueFilters = filterValueStore.findFilterValueFilterViewModel))
            }
          case _ => NotFound("No such celebrity")
        }
      }
    }
  }

  def getCreateCelebrityAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        implicit val flash = request.flash
        val (errorFields, fieldDefaults) = getCelebrityDetail()
        Ok(views.html.Application.admin.admin_celebritydetail(
          isCreate = true, errorFields = errorFields,
          fields = fieldDefaults,
          celebrity = None,
          currentFilterValueIds = Set[Long](),
          filterValueFilters = List[(FilterValue, Filter)]())
        )
      }
    }
  }

  private def getCelebrityDetail(celebrity: Option[Celebrity] = None
    )(implicit flash: play.api.mvc.Flash): (Option[List[String]], (String) => String) = {
    
    val errorFields = flash.get("errors").map(errString => errString.split(',').toList)

    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "celebrityId" => flash.get("celebrityId").getOrElse("")
        case "celebrityEmail" => flash.get("celebrityEmail").getOrElse("")
        case "bio" => StringEscapeUtils.escapeHtml4(flash.get("bio").getOrElse(""))
        case "casualName" => StringEscapeUtils.escapeHtml4(flash.get("casualName").getOrElse(""))
        case "organization" => StringEscapeUtils.escapeHtml4(flash.get("organization").getOrElse(""))
        case "publicName" => StringEscapeUtils.escapeHtml4(flash.get("publicName").getOrElse(""))
        case "publishedStatusString" => flash.get("publishedStatusString").getOrElse(PublishedStatus.Unpublished.toString)
        case "roleDescription" => StringEscapeUtils.escapeHtml4(flash.get("roleDescription").getOrElse(""))
        case "twitterUsername" => StringEscapeUtils.escapeHtml4(flash.get("twitterUsername").getOrElse(""))
        case _ => flash.get(paramName).getOrElse("")
      }
    } 

    (errorFields, fieldDefaults)
  }
}

