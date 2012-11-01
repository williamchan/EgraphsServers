package controllers.website.admin

import models._
import models.filters._
import enums.PublishedStatus
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import services.http.SafePlayParams.Conversions._
import services.logging.Logging
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import java.io.File
import services.logging.Logging
import java.text.SimpleDateFormat
import services.http.SafePlayParams.Conversions._
import play.api.mvc.MultipartFormData
import java.awt.image.BufferedImage
import services.{Dimensions, ImageUtil, Utils}
import services.mail.TransactionalMail
import services.blobs.Blobs.Conversions._
import org.apache.commons.mail.HtmlEmail
import play.api.mvc._

trait PostFilterValueAdminEndpoint {
  this: Controller =>
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def filterStore: FilterStore
  protected def filterValueStore: FilterValueStore
  
  case class PostFilterValueForm(
	  name: String,
	  publicName: String,
	  filterId : Long
  )
  
  def postFilterValueAdmin = postController() {
	  httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        val filterValueId = Form("filterValueId" -> longNumber).bindFromRequest.fold(formWithErrors => 0L, validForm => validForm)
        val filterId = Form("filterId" -> longNumber).bindFromRequest.fold(formWithErrors => 0L, validForm => validForm)
        val isCreate = (filterValueId == 0)
        
        val form = Form(mapping(
          "name" -> nonEmptyText(maxLength = 128),
          "publicName" -> nonEmptyText(maxLength = 128),
          "filterId" -> longNumber)(PostFilterValueForm.apply)(PostFilterValueForm.unapply)
          .verifying(isUniqueName(filterValueId)))  
        form.bindFromRequest.fold(
          formWithErrors => {
            val data = formWithErrors.data
            val errors = for (error <- formWithErrors.errors) yield {
              error.key + ": " + error.message
            }
            val url = if(isCreate) controllers.routes.WebsiteControllers.getCreateFilterValueAdmin(filterId).url else controllers.routes.WebsiteControllers.getFilterValueAdmin(filterValueId).url
            Redirect(url, SEE_OTHER).flashing( 
              ("errors" -> errors.mkString(", ")), 
  		        ("filterValueId" -> filterValueId.toString),
  		        ("publicName" -> data.get("publicName").getOrElse("")), 
  		        ("name" -> data.get("name").getOrElse(""))
            )
          },
          validForm => {
            val tmp = if (isCreate) FilterValue() else filterValueStore.get(filterValueId)
            val savedFilterValue = tmp.copy(
                publicName = validForm.publicName,
                name = validForm.name,
                filterId = validForm.filterId)
                .save()
            Redirect(controllers.routes.WebsiteControllers.getFilterAdmin(savedFilterValue.filterId).url, FOUND)
          }
        )
      }
	  }  
  }
	
  private def isUniqueName(filterValueId: Long): Constraint[PostFilterValueForm] = {
    Constraint { form: PostFilterValueForm => 
      filterValueStore.findByName(form.name) match {
        case None => Valid
        case Some(filterValue) if(filterValue.id == filterValueId) => Valid
        case _ => Invalid("Name must be unique")
      }
    }  
  }    	
}