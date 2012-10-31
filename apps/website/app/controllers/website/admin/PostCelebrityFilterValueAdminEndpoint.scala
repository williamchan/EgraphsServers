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
import play.api.data.validation._
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

/**
 * Controller for managing FilterValues associated with a celebrity
 */
trait PostCelebrityFilterValueAdminEndpoint {
  this: Controller => 
  
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def transactionalMail: TransactionalMail
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def filterValueStore: FilterValueStore
  
  case class CelebrityFilterValueForm(
      filterValueId: Long
  )

  def postCelebrityFilterValueAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        val form = Form(mapping(
          "filterValueId" -> longNumber
        )(CelebrityFilterValueForm.apply)(CelebrityFilterValueForm.unapply)
        .verifying(
          isValidFilterValueId
        ))
        
        form.bindFromRequest.fold(
          formWithErrors => {
            Redirect(controllers.routes.WebsiteControllers.getCelebritiesAdmin.url, BAD_REQUEST)
          },
          validForm => {
            celebrityStore.findById(celebrityId) match {
              case Some(celebrity) => {
                filterValueStore.findById(validForm.filterValueId) match {
                  case Some(filterValue) => {
                    celebrity.filterValues.associate(filterValue)
                    Redirect(controllers.routes.WebsiteControllers.getCelebritiesAdmin.url, CREATED)
                  }
                  case _ => Redirect(controllers.routes.WebsiteControllers.getCelebritiesAdmin.url, BAD_REQUEST)   
                }
              }  
              case _ =>  Redirect(controllers.routes.WebsiteControllers.getCelebritiesAdmin.url, BAD_REQUEST)   
            }      
          }
        )     
      }
    }  
  }

  private def isValidFilterValueId: Constraint[CelebrityFilterValueForm] = {
    Constraint { form: CelebrityFilterValueForm => 
      filterValueStore.findById(form.filterValueId) match {
        case Some(filterValue) => Valid
        case _ => Invalid("Filter Value ID is incorrect ")
      }
    }    
  }
}