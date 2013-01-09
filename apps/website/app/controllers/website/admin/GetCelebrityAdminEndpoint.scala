package controllers.website.admin

import models.{CelebrityAccesskey, AccountStore, CelebrityStore, Celebrity}
import models.enums.PublishedStatus
import models.categories._
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import controllers.website.consumer.CelebrityLandingConsumerEndpoint
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCelebrityAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore
  protected def categoryValueStore: CategoryValueStore  

  def getCelebrityAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
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
              val celebCategoryValueIds = (for(filterValue <- celebrity.categoryValues) yield { filterValue.id }).toSet

              val urlWithAccesskey = CelebrityAccesskey.urlWithAccesskey(
                controllers.routes.WebsiteControllers.getCelebrityLanding(celebrity.urlSlug).url,
                CelebrityAccesskey.accesskey(celebrity.id)
              )

              Ok(views.html.Application.admin.admin_celebritydetail(
                isCreate = false,
                errorFields = errorFields,
                fields = fieldDefaults,
                celebrity = Some(celebrity),
                urlWithAccesskey = Some(urlWithAccesskey),
                currentCategoryValueIds = celebCategoryValueIds,
                categoryValueCategories = categoryValueStore.findCategoryValueCategoryViewModel))
            }
          case _ => NotFound("No such celebrity")
        }
      }
    }
  }

  def getCreateCelebrityAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        implicit val flash = request.flash
        val (errorFields, fieldDefaults) = getCelebrityDetail()
        Ok(views.html.Application.admin.admin_celebritydetail(
          isCreate = true, errorFields = errorFields,
          fields = fieldDefaults,
          celebrity = None,
          currentCategoryValueIds = Set[Long](),
          categoryValueCategories = List[(CategoryValue, Category)]())
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
        case "bio" => (flash.get("bio").getOrElse(""))
        case "casualName" => flash.get("casualName").getOrElse("")
        case "organization" => flash.get("organization").getOrElse("")
        case "publicName" => flash.get("publicName").getOrElse("")
        case "publishedStatusString" => flash.get("publishedStatusString").getOrElse(PublishedStatus.Unpublished.toString)
        case "roleDescription" => flash.get("roleDescription").getOrElse("")
        case "twitterUsername" => flash.get("twitterUsername").getOrElse("")
        case _ => flash.get(paramName).getOrElse("")
      }
    } 

    (errorFields, fieldDefaults)
  }
}

