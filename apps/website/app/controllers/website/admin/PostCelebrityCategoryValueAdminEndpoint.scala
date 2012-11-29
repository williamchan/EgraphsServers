package controllers.website.admin

import models._
import models.categories._
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
 * This controller associates celebrities with CategoryValues i.e. "tags" the associated celeb.
 * It destructively dissociates a celeb from all of its categoryvalues and applys a new list from
 * the caller.
 */
trait PostCelebrityCategoryValueAdminEndpoint {
  this: Controller => 
  
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def transactionalMail: TransactionalMail
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def categoryValueStore: CategoryValueStore
  
  case class CelebrityCategoryValueForm(
      categoryValueId: Long
  )

  def postCelebrityCategoryValueAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        
        val categoryValueIds = request.body.asFormUrlEncoded match {
          case Some(params) if(params.contains("categoryValueIds")) => {
            for(categoryValueId <- params("categoryValueIds")) yield {
              categoryValueId.toLong
            }
          }
          case _ => List[Long]()
        }

        celebrityStore.findById(celebrityId) match {
            case Some(celebrity) => {
              celebrityStore.updateCategoryValues(celebrity = celebrity, categoryValueIds = categoryValueIds)
              Redirect(controllers.routes.WebsiteControllers.getCelebrityAdmin(celebrity.id).url, FOUND)
            }
            case _ => Redirect(controllers.routes.WebsiteControllers.getCelebritiesAdmin.url, SEE_OTHER)
          }
      }
    }  
  }
}