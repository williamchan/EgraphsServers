package models.checkout.data

import play.api.data.Form
import models.{OrderStore, Order}
import models.checkout.{EgraphOrderLineItemType, Checkout, LineItem, LineItemType}
import services.AppConfig
import services.db.HasTransientServices
import org.joda.money.{CurrencyUnit, Money}










object EgraphForm extends CheckoutForm[EgraphOrderLineItemType] {

  object FormKeys {
    val productId = "productId"
    val recipientName = "recipientName"
    val isGift = "isGift"
    val desiredText = "desiredText"
    val messageToCeleb = "messageToCeleb"
    val framedPrint = "framedPrint"
  }


  override val form: Form[EgraphOrderLineItemType] = {
    import play.api.data.Forms
    import Forms._
    import FormKeys._

    Form[EgraphOrderLineItemType](
      mapping(
        productId -> Forms.longNumber,
        recipientName -> nonEmptyText(3, 30),
        isGift -> boolean,
        desiredText -> optional(text(maxLength = 80)),
        messageToCeleb -> optional(text(maxLength = 180)),
        framedPrint -> boolean
      )(EgraphOrderLineItemType.apply)(EgraphOrderLineItemType.unapply)
    )
  }


  protected override val formErrorByField = {
    import ApiFormError._
    import FormKeys._
    Map(
      productId -> InvalidType,
      recipientName -> InvalidLength,
      isGift -> InvalidType,
      desiredText -> InvalidLength,
      messageToCeleb -> InvalidLength,
      framedPrint -> InvalidType
    )
  }
}
