package services.http.filters

import com.google.inject.Inject
import models.{Product, ProductStore}
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound

// TODO: PLAY20 migration. Comment this summbitch.
class RequireProductId @Inject() (productStore: ProductStore) {

  def apply[A](productId: Long, parser: BodyParser[A] = parse.anyContent)(actionFactory: Product => Action[A])
  : Action[A] = 
  {
    Action(parser) { request =>     
      val maybeResult = for (
        product <- productStore.findById(productId)
      ) yield {
        actionFactory(product).apply(request)
      }
      
      maybeResult.getOrElse(noProductIdResult)
    }
  } 

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: Product => Action[A])
  : Action[A] = 
  {
    Action(parser) { implicit request =>
      Form(single("productId" -> longNumber)).bindFromRequest.fold(
        errors => noProductIdResult,
        productId => this.apply(productId, parser)(actionFactory)(request)
      )
    }
  }
  
  //
  // Private members
  //
  private val noProductIdResult = NotFound("Valid Product ID was required but not provided")
}
