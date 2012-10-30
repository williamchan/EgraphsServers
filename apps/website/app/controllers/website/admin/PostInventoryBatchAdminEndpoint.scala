package controllers.website.admin

import java.util.Date
import models._
import controllers.WebsiteControllers
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

trait PostInventoryBatchAdminEndpoint extends Logging {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def inventoryBatchStore: InventoryBatchStore
  
  def postCreateInventoryBatchAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { celebrity =>
        Action { implicit request =>
          
          val form = Form(mapping(
              "numInventory" -> number, // TODO: verify this is non-negative
              "startDate" -> date,
              "endDate" -> date
          )(PostInventoryBatchForm.apply)(PostInventoryBatchForm.unapply))
          
          form.bindFromRequest.fold(formWithErrors => {
            val data = formWithErrors.data
            val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
            val url = controllers.routes.WebsiteControllers.getCreateCelebrityProductAdmin(celebrityId).url
            Redirect(url).flashing(
	            ("errors" -> errors.mkString(", ")), 
	            ("numInventory" -> data.get("numInventory").getOrElse("")), 
	            ("startDate" -> data.get("startDate").getOrElse("")), 
	            ("endDate" -> data.get("endDate").getOrElse(""))
	          )
	      }, validForm => {
	        val inventoryBatch = InventoryBatch(celebrityId = celebrity.id, 
                numInventory = validForm.numInventory, startDate = validForm.startDate, endDate = validForm.endDate).save()
            
            val productsSelected = getSelectedProducts(celebrity, request.body.asFormUrlEncoded.get)
            for (p <- productsSelected) inventoryBatch.products.associate(p)

            Redirect(GetCelebrityInventoryBatchesAdminEndpoint.url(celebrityId = celebrityId))
          })
      	}
      }
    }
  }
  
  def postInventoryBatchAdmin(inventoryBatchId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireInventoryBatchId(inventoryBatchId) { inventoryBatch =>
        Action { implicit request =>
          
          val celebrity = inventoryBatch.celebrity
          val form = Form(mapping(
              "numInventory" -> number, // TODO: verify this is non-negative
              "startDate" -> date,
              "endDate" -> date
          )(PostInventoryBatchForm.apply)(PostInventoryBatchForm.unapply))  
          
          form.bindFromRequest.fold(formWithErrors => {
            val data = formWithErrors.data
	        val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
            val url = controllers.routes.WebsiteControllers.getInventoryBatchAdmin(inventoryBatchId).url
            Redirect(url).flashing(
	            ("errors" -> errors.mkString(", ")), 
	            ("numInventory" -> data.get("numInventory").getOrElse("")), 
	            ("startDate" -> data.get("startDate").getOrElse("")), 
	            ("endDate" -> data.get("endDate").getOrElse(""))
	          )
	      }, validForm => {
	        val savedInventoryBatch = inventoryBatch.copy(
	            numInventory = validForm.numInventory, startDate = validForm.startDate, endDate = validForm.endDate).save()
            
            val productsSelected = getSelectedProducts(celebrity, request.body.asFormUrlEncoded.get)
            // todo(wchan): This logic is generally useful for ManyToManys
            val productsAlreadyInInventoryBatch = savedInventoryBatch.products.toList
            val productsToAssociate = productsSelected.diff(productsAlreadyInInventoryBatch)
            val productsToDissociate = productsAlreadyInInventoryBatch.diff(productsSelected)
            for (productToAssociate <- productsToAssociate) { savedInventoryBatch.products.associate(productToAssociate) }
	        for (productToDissociate <- productsToDissociate) { savedInventoryBatch.products.dissociate(productToDissociate) }
	        
	        Redirect(GetCelebrityInventoryBatchesAdminEndpoint.url(celebrityId = celebrity.id))
          })
      	}
      }
    }
  }
  
  private case class PostInventoryBatchForm(numInventory: Int, startDate: Date, endDate: Date)
  
  private def getSelectedProducts(celebrity: Celebrity, rawFormData: Map[String, Seq[String]]): List[Product] = {
    val products = celebrity.products().toList
    products.filter(p => rawFormData.contains("prod" + p.id))
  }
}
