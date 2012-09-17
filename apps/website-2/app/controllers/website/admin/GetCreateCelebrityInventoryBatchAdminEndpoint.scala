package controllers.website.admin

import play.api.mvc.Controller
import services.Utils
import models.Celebrity
import services.http.{ControllerMethod, AdminRequestFilters}
import org.joda.time.DateTime
import java.text.SimpleDateFormat

private[controllers] trait GetCreateCelebrityInventoryBatchAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getCreateCelebrityInventoryBatchAdmin = controllerMethod() {
    adminFilters.requireCelebrity { (celebrity, admin) =>

      val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
      val today = DateTime.now().toLocalDate.toDate
      val twoWeeksHence = new DateTime().plusDays(14).toLocalDate.toDate
      flash.put("startDate", dateFormat.format(today))
      flash.put("endDate", dateFormat.format(twoWeeksHence))
      GetInventoryBatchDetail.getCelebrityInventoryBatchDetail(celebrity = celebrity)
    }
  }
}

object GetCreateCelebrityInventoryBatchAdminEndpoint {

  def url(celebrity: Celebrity) = {
    Utils.lookupUrl("WebsiteControllers.getCreateCelebrityInventoryBatchAdmin", Map("celebrityId" -> celebrity.id.toString))
  }
}
