package models.checkout.forms

import play.api.data.Form
import models.checkout._
import services.AppConfig.instance


object EgraphForm extends CheckoutForm[EgraphOrderLineItemType] {

  object FormKeys {
    val productId = "productId"
    val recipientName = "recipientName"
    val isGift = "isGift"
    val desiredText = "desiredText"
    val messageToCeleb = "messageToCeleb"
    val framedPrint = "framedPrint"
  }


  override val form = Form[EgraphOrderLineItemType] {
    import play.api.data.Forms.{ignored, mapping, optional}
    import ApiForms._
    import FormKeys._

    // TODO use dependency injector to apply services instead of static call
    mapping(
      productId -> longNumber.verifying(validProductId),
      recipientName -> text(3, 30),
      isGift -> boolean,
      desiredText -> optional(text(max = maxDesiredTextChars)),
      messageToCeleb -> optional(text(max = maxMessageToCelebChars)),
      framedPrint -> boolean,
      "_services" -> ignored(instance[EgraphOrderLineItemTypeServices])
    )(EgraphOrderLineItemType.apply)(EgraphOrderLineItemType.unapply)

  }

  val maxMessageToCelebChars = 180
  val maxDesiredTextChars = 60
}
