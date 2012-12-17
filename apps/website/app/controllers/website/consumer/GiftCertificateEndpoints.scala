package controllers.website.consumer

import play.api.mvc.{Action, Controller, Request}

import org.joda.money.{CurrencyUnit, Money}

import services.http.ControllerMethod
import services.http.POSTControllerMethod
import services.mvc.ImplicitHeaderAndFooterData

import models.checkout.{/*StripeChargeLineItemType,*/ Checkout, GiftCertificateLineItemTypeServices, GiftCertificateLineItemType}


/**
 */
private[consumer] trait GiftCertificateEndpoints
  extends ImplicitHeaderAndFooterData {
  this: Controller =>
  import GiftCertificateLineItemTypeServices.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod

  // Get gift certificate page
  def getGiftCertificatePage = controllerMethod.withForm() { implicit authToken =>
    Action {
      // TODO(SER-499): Use actual view
      Ok("Gift certificate page.")
    }
  }

  //
  // TODO(SER-499): JSON endpoint for summary
  // for "Review Order" after finalizing order
  def postGiftCertificateSummaryJson = postController() {
    Action { implicit request =>
      // TODO(SER-499): actually return json
      Ok
    }
  }


  /**
   * Reaching this endpoint either:
   * -marks the completion of purchasing a gift certificate
   * -returns to gift certificate page due to form errors
   */
  def postGiftCertificatePage = postController() {
    Action { implicit request =>
      // redirect to gift certificate purchase receipt or return to same page with errors
      // TODO(SER-499): actually redirect, follow pattern used in other controllers
      Ok
    }
  }

  /**
   * @return Page summarizing transaction
   */
  def getGiftCertificateReceipt = controllerMethod.withForm() { implicit authToken =>
    // TODO(SER-499): return actual receipt page
    Action { Ok("Gift certificate receipt page.") }
  }


  protected def maybeCheckout(request: Request[_]): Option[Checkout] = {
    // TODO(SER-499): get actual form from request
    val form = new {
      def amount: Option[Money] = Some(Money.parse("$50"))
      def recipientName: Option[String] = Some("Johnny Boy")
      def stripeId: Option[String] = Some("934texas")
    }

    val checkout = for (
      amount <- form.amount;
      recipient <- form.recipientName;
      stripeId <- form.stripeId

    ) yield {
      val giftCertificate = GiftCertificateLineItemType.getWithRecipientAndAmount(recipient, amount)
      Checkout(IndexedSeq(giftCertificate))
    }
    checkout.headOption
  }
}