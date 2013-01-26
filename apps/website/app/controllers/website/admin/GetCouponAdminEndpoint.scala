package controllers.website.admin

import egraphs.authtoken.AuthenticityToken
import java.text.SimpleDateFormat
import models.{Coupon, CouponStore}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc._
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCouponAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def couponStore: CouponStore
  private def dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm")
  // TODO SER-504: Hack to make startDate and endDate PST.
  private def eightHoursInMillis = 28800000 // AWS servers think they're in GMT regardless of region

  def getCouponAdmin(couponId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        couponStore.findById(couponId) match {
          case Some(coupon) =>
            getCouponDetailHtml(coupon)
          case None => NotFound("No such coupon")
        }
      }
    }
  }

  def getCreateCouponAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        getCouponDetailHtml(Coupon())
      }
    }
  }
  
  private def getCouponDetailHtml(coupon: Coupon)(implicit request: RequestHeader, authToken: AuthenticityToken): Result = {
	val errorFields = flash.get("errors").map(errString => errString.split(',').toList)

    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "couponId" => coupon.id.toString
        case "name" => coupon.name
        case "code" => coupon.code
        case "startDate" => dateFormat.format(coupon.startDate.getTime - eightHoursInMillis)
        case "endDate" => dateFormat.format(coupon.endDate.getTime - eightHoursInMillis)
        case "discountAmount" => coupon.discountAmount.toInt.toString
        
        case _ => flash.get(paramName).getOrElse("")
      }
    }
    
	val isCreate = (coupon.id == 0)
	Ok(views.html.Application.admin.admin_coupondetail(
	  isCreate = isCreate,
	  errorFields = errorFields,
	  fields = fieldDefaults,
	  coupon = coupon)
	)
  }
}
