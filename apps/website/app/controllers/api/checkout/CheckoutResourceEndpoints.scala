package controllers.api.checkout

import play.api.mvc.Controller
import models.checkout.forms._
import models.checkout._
import models.checkout.EgraphCheckoutAdapter

trait CheckoutResourceEndpoints { this: Controller =>
  //
  // Services
  //
  protected def checkoutControllers: CheckoutResourceControllerFactory
  
  //
  // Controllers
  //
  lazy val getCheckoutBuyer = buyer.get _
  lazy val postCheckoutBuyer = buyer.post _

  lazy val getCheckoutRecipient = recipient.get _
  lazy val postCheckoutRecipient = recipient.post _

  lazy val getCheckoutCoupon = coupon.get _
  lazy val postCheckoutCoupon = coupon.post _
  
  lazy val getCheckoutEgraph = egraph.get _
  lazy val postCheckoutEgraph = egraph.post _

  lazy val getCheckoutShippingAddress = shippingAddress.get _
  lazy val postCheckoutShippingAddress = shippingAddress.post _
  
  lazy val getCheckoutPayment = payment.get _
  lazy val postCheckoutPayment = payment.post _

  //
  // Private Members
  //
  private lazy val buyer = emailResource((resource, checkout) => checkout.withBuyerEmail(resource))

  private lazy val recipient = emailResource((resource, checkout) => checkout.withRecipientEmail(resource))

  private lazy val coupon = checkoutControllers[CouponLineItemType](
    CouponForm,
    (resource, checkout) => checkout.withCoupon(resource)
  )

  private lazy val egraph = checkoutControllers[EgraphOrderLineItemType](
    EgraphForm,
    (resource, checkout) => checkout.withOrder(resource)
  )

  private lazy val shippingAddress = checkoutControllers[ShippingAddress](
    ShippingAddressForm,
    (resource, checkout) => checkout.withShippingAddress(resource)
  )

  private lazy val payment = checkoutControllers[CashTransactionLineItemType](
    PaymentForm,
    (resource, checkout) => checkout.withPayment(resource)
  )

  private def emailResource(setter: (Option[String], EgraphCheckoutAdapter) => EgraphCheckoutAdapter)
  : CheckoutResourceController[String] =
  {
    checkoutControllers[String](EmailForm, setter)
  }
}


