package controllers.website.admin

import controllers.WebsiteControllers
import java.sql.Timestamp
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import models._
import models.enums.{CouponType, CouponDiscountType, CouponUsageType}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import services.http.SafePlayParams.Conversions._
import services.logging.Logging
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid

trait PostCouponAdminEndpoint extends Logging {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def couponStore: CouponStore
  private def dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm")
  // TODO SER-504: Hack to make startDate and endDate PST.
  private def eightHours = 28800000
  
  def postCreateCouponAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val form = Form(mapping(
                "name" -> text,
                "code" -> nonEmptyText,
                "startDate" -> nonEmptyText.verifying(isValidDateAndTime),
                "endDate" -> nonEmptyText.verifying(isValidDateAndTime),
                "discountAmount" -> number,
                "couponTypeString" -> nonEmptyText.verifying(isCouponType),
                "discountTypeString" -> nonEmptyText.verifying(isDiscountType),
                "usageTypeString" -> nonEmptyText.verifying(isUsageType),
                "restrictions" -> nonEmptyText
        )(PostCouponForm.apply)(PostCouponForm.unapply)
          verifying(codeIsUnique())
        )
          
        form.bindFromRequest.fold(formWithErrors => {
          val data = formWithErrors.data
          val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
          val url = controllers.routes.WebsiteControllers.getCreateCouponAdmin.url
          Redirect(url).flashing(("errors" -> errors.mkString(", ")))
	    }, validForm => {
	      // enums have already been validated to exist
	      val couponType = CouponType(validForm.couponTypeString).get
	      val discountType = CouponDiscountType(validForm.discountTypeString).get
	      val usageType = CouponUsageType(validForm.usageTypeString).get
	      val startDateTimestamp = new Timestamp(dateFormat.parse(validForm.startDate).getTime + eightHours)
	      val endDateTimestamp = new Timestamp(dateFormat.parse(validForm.endDate).getTime + eightHours)
	      val coupon = Coupon(
	          name = validForm.name,
	          code = validForm.code,
	          startDate = startDateTimestamp,
	          endDate = endDateTimestamp,
	          discountAmount = validForm.discountAmount,
	          restrictions = validForm.restrictions
	      ).withCouponType(couponType).withDiscountType(discountType).withUsageType(usageType).save()
	      
          Redirect(controllers.routes.WebsiteControllers.getCouponAdmin(coupon.id).url)
        })
      }
    }
  }
  
  def postCouponAdmin(couponId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        couponStore.findById(couponId) match {
          case None => NotFound("No coupon with that id")
          case Some(coupon) => {
            val form = Form(mapping(
                "name" -> text,
                "code" -> nonEmptyText,
                "startDate" -> nonEmptyText.verifying(isValidDateAndTime),
                "endDate" -> nonEmptyText.verifying(isValidDateAndTime),
                "discountAmount" -> number,
                "couponTypeString" -> nonEmptyText.verifying(isCouponType),
                "discountTypeString" -> nonEmptyText.verifying(isDiscountType),
                "usageTypeString" -> nonEmptyText.verifying(isUsageType),
                "restrictions" -> nonEmptyText
            )(PostCouponForm.apply)(PostCouponForm.unapply)
              verifying(codeIsUnique(Some(couponId)))
            )
            
            form.bindFromRequest.fold(formWithErrors => {
              val data = formWithErrors.data
	          val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
              val url = controllers.routes.WebsiteControllers.getCouponAdmin(couponId).url
              Redirect(url).flashing(("errors" -> errors.mkString(", ")))
	        }, validForm => {
	          // enums have already been validated to exist
		      val couponType = CouponType(validForm.couponTypeString).get
		      val discountType = CouponDiscountType(validForm.discountTypeString).get
		      val usageType = CouponUsageType(validForm.usageTypeString).get
		      val startDateTimestamp = new Timestamp(dateFormat.parse(validForm.startDate).getTime + eightHours)
		      val endDateTimestamp = new Timestamp(dateFormat.parse(validForm.endDate).getTime + eightHours)
		      coupon.copy(
		          name = validForm.name,
		          code = validForm.code,
		          startDate = startDateTimestamp,
		          endDate = endDateTimestamp,
		          discountAmount = validForm.discountAmount,
		          restrictions = validForm.restrictions
		      ).withCouponType(couponType).withDiscountType(discountType).withUsageType(usageType).save()
		      
              Redirect(controllers.routes.WebsiteControllers.getCouponAdmin(couponId).url)
            })
          }
      	}
      }
    }
  }
  
  private case class PostCouponForm(
      name: String, 
      code: String, 
      startDate: String,
      endDate: String,
      discountAmount: Int,
      couponTypeString: String,
      discountTypeString: String,
      usageTypeString: String,
      restrictions: String
  )
  
  private def codeIsUnique(couponId: Option[Long] = None): Constraint[PostCouponForm] = {
    Constraint { form: PostCouponForm =>
      (couponStore.findValid(form.code), couponId) match {
        case (None, _) => Valid
        case (Some(coupon), Some(id)) if (id == coupon.id) => Valid
        case _ => Invalid("An active coupon with that code already exists.")
      }
    }
  }
  
  private def isValidDateAndTime: Constraint[String] = {
    Constraint { s: String =>
      try{
        dateFormat.parse(s)
        Valid
      } catch { case e: ParseException => Invalid("Date was not correctly formatted") }
    }
  }
  
  private def isCouponType: Constraint[String] = {
    Constraint { s: String =>
      CouponType(s) match {
        case Some(providedType) => Valid
        case None => Invalid("Error setting coupon type, please contact support")
      }
    }
  }
  
  private def isDiscountType: Constraint[String] = {
    Constraint { s: String =>
      CouponDiscountType(s) match {
        case Some(providedType) => Valid
        case None => Invalid("Error setting discount type, please contact support")
      }
    }
  }
  
  private def isUsageType: Constraint[String] = {
    Constraint { s: String =>
      CouponUsageType(s) match {
        case Some(providedType) => Valid
        case None => Invalid("Error setting coupon usage type, please contact support")
      }
    }
  }
}
