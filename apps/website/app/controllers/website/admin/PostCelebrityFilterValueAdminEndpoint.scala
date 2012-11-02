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
 * This controller associates celebrities with FilterValues i.e. "tags" the associated celeb.
 * It destructively dissociates a celeb from all of its filtervalues and applys a new list from
 * the caller.
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
        
        val filterValueIds = request.body.asFormUrlEncoded match {
          case Some(params) if(params.contains("filterValueIds")) => {
            for(filterValueId <- params("filterValueIds")) yield {
              filterValueId.toLong
            }
          }
          case _ => List[Long]()
        }

        celebrityStore.findById(celebrityId) match {
        case Some(celebrity) => {
              celebrityStore.updateFilterValues(celebrity = celebrity, filterValueIds = filterValueIds) 
              Redirect(controllers.routes.WebsiteControllers.getCelebrityAdmin(celebrity.id).url, FOUND)
            }
            case _ => Redirect(controllers.routes.WebsiteControllers.getCelebritiesAdmin.url, SEE_OTHER)   
          }
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