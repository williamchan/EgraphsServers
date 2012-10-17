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

trait PostCelebrityInventoryBatchAdminEndpoint extends Logging {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def inventoryBatchStore: InventoryBatchStore

  // TODO: This controller could use some rewriting!
  def postCelebrityInventoryBatchAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { celebrity =>
        Action { implicit request =>
          
          val form = Form(tuple(
              "inventoryBatchId" -> longNumber,
              "numInventory" -> number, // TODO: verify this is non-negative
              "startDate" -> date,
              "endDate" -> date
          ))
          form.bindFromRequest.fold(hasErrors => {
            val badData = hasErrors.value.get
            Redirect(controllers.routes.WebsiteControllers.postCelebrityInventoryBatchAdmin(celebrityId)).
            flashing("errors" -> hasErrors.errors.head.message.toString(),
                "inventoryBatchId" -> badData._1.toString, "numInventory" -> badData._2.toString, "startDate" -> badData._3.toString, "endDate" -> badData._4.toString)

          }, success => {
            val inventoryBatchId = success._1
            val numInventory = success._2
            val startDate = success._3
            val endDate = success._4
            val isCreate = (inventoryBatchId == 0)

            // Need to get the select prod #s, which are not currently handled by the form
            val rawFormData = request.body.asFormUrlEncoded.get
            val products = celebrity.products().toList
            val productsSelected = products.filter(p => rawFormData.contains("prod" + p.id))

            if (isCreate) {
              val inventoryBatch = InventoryBatch(celebrityId = celebrity.id, numInventory = numInventory, startDate = startDate, endDate = endDate).save()
              for (p <- productsSelected) inventoryBatch.products.associate(p)
            } else {
              val ib = inventoryBatchStore.get(inventoryBatchId)
              val inventoryBatch = ib.copy(numInventory = numInventory, startDate = startDate, endDate = endDate).save()
              // todo(wchan): This logic is generally useful for ManyToManys
              val productsAlreadyInInventoryBatch = inventoryBatch.products.toList
              val productsToAssociate = productsSelected.diff(productsAlreadyInInventoryBatch)
              val productsToDissociate = productsAlreadyInInventoryBatch.diff(productsSelected)
              for (productToAssociate <- productsToAssociate) { inventoryBatch.products.associate(productToAssociate) }
              for (productToDissociate <- productsToDissociate) { inventoryBatch.products.dissociate(productToDissociate) }
            }

            Redirect(GetCelebrityInventoryBatchesAdminEndpoint.url(celebrityId = celebrityId))
          })
      	}
      }
    }
  }
}
