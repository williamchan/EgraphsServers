package services.http.filters

import com.google.inject.Inject
import models.{InventoryBatch, InventoryBatchStore}
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound

// TODO: PLAY20 migration. Comment this summbitch.
class RequireInventoryBatchId @Inject() (inventoryBatchStore: InventoryBatchStore) {

  def apply[A](inventoryBatchId: Long, parser: BodyParser[A] = parse.anyContent)(actionFactory: InventoryBatch => Action[A])
  : Action[A] = 
  {
    Action(parser) { request =>     
      val maybeResult = for (
        inventoryBatch <- inventoryBatchStore.findById(inventoryBatchId)
      ) yield {
        actionFactory(inventoryBatch).apply(request)
      }
      
      maybeResult.getOrElse(noInventoryBatchIdResult)
    }
  } 

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: InventoryBatch => Action[A])
  : Action[A] = 
  {
    Action(parser) { implicit request =>
      Form(single("inventoryBatchId" -> longNumber)).bindFromRequest.fold(
        errors => noInventoryBatchIdResult,
        inventoryBatchId => this.apply(inventoryBatchId, parser)(actionFactory)(request)
      )
    }
  }
  
  //
  // Private members
  //
  private val noInventoryBatchIdResult = NotFound("Valid InventoryBatch ID was required but not provided")
}
