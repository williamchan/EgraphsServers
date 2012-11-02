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


trait PostFilterAdminEndpoint {
  this: Controller =>    

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def filterStore: FilterStore
  
  case class PostFilterForm(
     name: String,
     publicName: String
  )

  def postFilterAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession() 
    { (admin, adminAccount) =>
      Action { implicit request =>
        
        val filterId = Form("filterId" -> longNumber).bindFromRequest.fold(formWithErrors => 0L, validForm => validForm)
      	val isCreate = (filterId == 0)
      	
      	val form = Form(mapping(
          "name" -> nonEmptyText(maxLength = 128),
          "publicName" -> nonEmptyText(maxLength = 128)
        )(PostFilterForm.apply)(PostFilterForm.unapply).verifying(isUniqueName(filterId))
        )
        
        form.bindFromRequest.fold(
          formWithErrors => {
            val data = formWithErrors.data
            val errors = for (error <- formWithErrors.errors) yield {
              error.key + ": " + error.message
            }
            val url = if(isCreate) controllers.routes.WebsiteControllers.getCreateFilterAdmin.url else controllers.routes.WebsiteControllers.getFilterAdmin(filterId).url
            Redirect(url, SEE_OTHER).flashing(
              ("errors" -> errors.mkString(", ")), 
  		        ("filterId" -> filterId.toString), 
  		        ("publicName" -> data.get("publicName").getOrElse("")), 
  		        ("name" -> data.get("name").getOrElse(""))
            )
          },
          validForm => {
            val tmp = if (isCreate) Filter() else filterStore.get(filterId)
            val savedFilter = tmp.copy(
                publicName = validForm.publicName,
                name = validForm.name).save()
            Redirect(controllers.routes.WebsiteControllers.getFilterAdmin(savedFilter.id).url, FOUND)
          }
        )
      }
  	}
  }
  
  private def isUniqueName(filterId: Long): Constraint[PostFilterForm] = {
    Constraint { form: PostFilterForm => 
      filterStore.findByName(form.name) match {
        case None => Valid
        case Some(filter) if(filter.id == filterId) => Valid
        case _ => Invalid("Name must be unique")
      }
    }  
  }    
}