package controllers.website.consumer

import services.http.{SafePlayParams, POSTControllerMethod, CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import services.http.forms.purchase._
import models.enums.{PrintingOption, WrittenMessageRequest}
import PrintOptionForm.Params
import SafePlayParams.Conversions._
import services.mvc.FormConversions.{checkoutBillingFormToViewConverter, checkoutShippingFormToViewConverter}
import models.frontend.storefront.{CheckoutFormView, CheckoutOrderSummary, CheckoutBillingInfoView, CheckoutShippingAddressFormView}
import services.payment.Payment

/**
 * Endpoint for serving up the Choose Photo page
 */
private[consumer] trait StorefrontCheckoutConsumerEndpoints
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod
  protected def purchaseFormFactory: PurchaseFormFactory
  protected def purchaseFormReaders: PurchaseFormReaders
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def checkPurchaseField: PurchaseFormChecksFactory
  protected def payment: Payment

  def getStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      val forms = purchaseFormFactory.formsForStorefront(celeb.id)

      for (
        // Make sure the product ID matches
        formProductId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;

        // Make sure there's inventory
        inventoryBatch <- forms.nextInventoryBatchOrRedirect(celebrityUrlSlug, product).right;

        // Make sure we've got a personalize form in storage, or redirect to personalize
        validPersonalizeForm <- forms.validPersonalizeFormOrRedirectToPersonalizeForm(
                                  celebrityUrlSlug,
                                  productUrlSlug
                                ).right;

        // Make sure we've got a high-quality print option from the Review page, or redirect to it
        highQualityPrint <- forms.highQualityPrintOrRedirectToReviewForm(
                              celebrityUrlSlug,
                              productUrlSlug
                            ).right
      ) yield {
        val maybeShippingFormView = highQualityPrint match {
          case PrintingOption.HighQualityPrint =>
            val restoredOrDefaultShipping = forms.shippingForm(Some(flash)).map {
              shipping => shipping.asCheckoutPageView
            }.getOrElse {
              defaultShippingView
            }

            Some(restoredOrDefaultShipping)

          case PrintingOption.DoNotPrint =>
            None
        }

        val billingFormView = forms.billingForm(Some(flash)).map { billing =>
          billing.asCheckoutPageView
        }.getOrElse {
          defaultBillingView
        }

        val orderSummary = CheckoutOrderSummary(
          celebrityName=celeb.publicName.getOrElse("Anonymous"),
          productName=product.name,
          recipientName=validPersonalizeForm.recipientName,
          messageText=makeTextForCelebToWrite(
            validPersonalizeForm.writtenMessageRequest,
            validPersonalizeForm.writtenMessageText
          ),
          basePrice=product.price,
          shipping=forms.shippingPrice,
          tax=forms.tax,
          total=forms.total(product.price)
        )

        val checkoutFormView = CheckoutFormView(
          actionUrl=reverse(postStorefrontCheckout(celebrityUrlSlug, productUrlSlug)).url,
          billing=billingFormView,
          shipping=maybeShippingFormView
        )

        views.frontend.html.celebrity_storefront_checkout(
          form=checkoutFormView,
          summary=orderSummary,
          paymentPublicKey=payment.publishableKey
        )
      }
    }
  }

  def postStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = postController() {
    celebFilters.requireCelebrityAndProductUrlSlugs {
      (celeb, product) =>
        val forms = purchaseFormFactory.formsForStorefront(celeb.id)

        for (
          productId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;
          validPrintOption <- checkPurchaseField(params.getOption(Params.HighQualityPrint))
            .isPrintingOption
            .right
        ) yield {
          forms.withHighQualityPrint(validPrintOption).save()


        }
    }
  }

  private def makeTextForCelebToWrite(messageRequest: WrittenMessageRequest, messageText: Option[String])
  : String = {
    messageRequest match {
      // TODO: Make these strings respond to gender
      case WrittenMessageRequest.SignatureOnly => "His signature only."
      case WrittenMessageRequest.CelebrityChoosesMessage => "Whatever he wants."
      case WrittenMessageRequest.SpecificMessage => messageText.getOrElse("")
    }
  }

  private def defaultBillingView:CheckoutBillingInfoView = {
    import CheckoutBillingForm.Params._
    CheckoutBillingInfoView.empty(Name, Email, PostalCode)
  }

  private def defaultShippingView: CheckoutShippingAddressFormView = {
    import CheckoutShippingForm.Params._
    CheckoutShippingAddressFormView.empty(
      Name,
      Email,
      AddressLine1,
      AddressLine2,
      City,
      State,
      PostalCode,
      BillingIsSame
    )
  }
}