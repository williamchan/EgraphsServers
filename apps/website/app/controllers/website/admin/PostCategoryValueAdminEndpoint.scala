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
/**
 * This controller manages the associations category values have with categories as well as basic data
 * around the category value object in the database.
 * CategoryValues have one parent Category; this controller allows the user to set the children Categories
 * of a CategoryValue. 
 * 
 */
trait PostCategoryValueAdminEndpoint {
  this: Controller =>
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def categoryStore: CategoryStore
  protected def categoryValueStore: CategoryValueStore
  
  case class PostCategoryValueForm(
	  name: String,
	  publicName: String
  )
  
  def postCategoryValueAdmin = postController() {
	httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val categoryValueId = Form("categoryValueId" -> longNumber).bindFromRequest.fold(formWithErrors => 0L, validForm => validForm)
        val categoryId = Form("categoryId" -> longNumber).bindFromRequest.fold(formWithErrors => 0L, validForm => validForm)
        val isCreate = (categoryValueId == 0)
        
        val form = Form(mapping(
          "name" -> nonEmptyText(maxLength = 128),
          "publicName" -> nonEmptyText(maxLength = 128)
        )(PostCategoryValueForm.apply)(PostCategoryValueForm.unapply)
          .verifying(isUniqueName(categoryValueId)))  
        form.bindFromRequest.fold(
          formWithErrors => {
            val data = formWithErrors.data
            val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
            val url = if(isCreate) controllers.routes.WebsiteControllers.getCreateCategoryValueAdmin(categoryId).url else controllers.routes.WebsiteControllers.getCategoryValueAdmin(categoryValueId).url
            Redirect(url, SEE_OTHER).flashing( 
              ("errors" -> errors.mkString(", ")), 
  		        ("categoryValueId" -> categoryValueId.toString),
  		        ("publicName" -> data.get("publicName").getOrElse("")), 
  		        ("name" -> data.get("name").getOrElse(""))
            )
          },
          validForm => {
            val tmp = if (isCreate) CategoryValue() else categoryValueStore.get(categoryValueId)
            val categoryIds = request.body.asFormUrlEncoded match {
              case Some(params) if(params.contains("categoryIds")) => {
                for(categoryId <- params("categoryIds")) yield {
                  categoryId.toLong
                }
              }
              case _ => List[Long]()
            }

            val savedCategoryValue = tmp.copy(
                publicName = validForm.publicName,
                name = validForm.name,
                categoryId = categoryId)
                .save()
            categoryValueStore.updateCategories(savedCategoryValue, categoryIds)
            Redirect(controllers.routes.WebsiteControllers.getCategoryAdmin(categoryId).url, FOUND)
          }
        )
      }
	  }  
  }
	
  private def isUniqueName(categoryValueId: Long): Constraint[PostCategoryValueForm] = {
    Constraint { form: PostCategoryValueForm => 
      categoryValueStore.findByName(form.name) match {
        case None => Valid
        case Some(categoryValue) if(categoryValue.id == categoryValueId) => Valid
        case _ => Invalid("Name must be unique")
      }
    }  
  }    	
}