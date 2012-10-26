package controllers.website.admin

import play.api.mvc.Controller
import models.Celebrity
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCreateCelebrityInventoryBatchAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def getCreateCelebrityInventoryBatchAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { implicit celebrity =>
        Action { implicit request =>
          val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
          val today = DateTime.now().toLocalDate.toDate
          val twoWeeksHence = new DateTime().plusDays(14).toLocalDate.toDate
          implicit val flash = request.flash + 
          ("startDate" -> dateFormat.format(today)) + 
          ("endDate" -> dateFormat.format(twoWeeksHence))
          GetInventoryBatchDetail.getCelebrityInventoryBatchDetail(celebrity = celebrity)
        }
      }
    }
  }
}

object GetCreateCelebrityInventoryBatchAdminEndpoint {

  def url(celebrity: Celebrity) = {
    controllers.routes.WebsiteControllers.getCreateCelebrityInventoryBatchAdmin(celebrityId = celebrity.id).url
  }
}
