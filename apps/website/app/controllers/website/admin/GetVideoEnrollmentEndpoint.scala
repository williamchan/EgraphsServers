package controllers.website.admin

import services.mvc.ImplicitHeaderAndFooterData
import play.api._
import play.api.mvc._

private[controllers] trait GetVideoEnrollmentEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  def getVideoEnrollment = Action {
    Ok(views.html.Application.admin.admin_videoasset())
  }


}