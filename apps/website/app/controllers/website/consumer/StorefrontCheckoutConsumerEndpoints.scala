package controllers.website.consumer

import services.http.{SafePlayParams, POSTControllerMethod, CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import services.http.forms.purchase._
import models.enums.{PrintingOption, WrittenMessageRequest}
import services.http.forms.Form.Conversions._
import services.mvc.FormConversions.{checkoutBillingFormToViewConverter, checkoutShippingFormToViewConverter}
import models.frontend.storefront.{CheckoutFormView, CheckoutOrderSummary, CheckoutBillingInfoView, CheckoutShippingAddressFormView}
import services.payment.Payment
import play.mvc.results.Redirect
import controllers.website.PostBuyProductEndpoint.EgraphPurchaseHandler

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
        val shippingFormReader = purchaseFormReaders.forShippingForm

        for (
          // Product ID in the url must match the product being ordered, or redirect to photo
          productId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;

          // There must be inventory
          inventoryBatch <- forms.nextInventoryBatchOrRedirect(celebrityUrlSlug, product).right;

          // Gotta have a valid personalize form in storage
          validPersonalizeForm <- forms.validPersonalizeFormOrRedirectToPersonalizeForm(
                                    celebrityUrlSlug,
                                    productUrlSlug
                                  ).right;

          // And a printing option from the review page
          printingOption <- forms.highQualityPrintOrRedirectToReviewForm(
                                celebrityUrlSlug,
                                productUrlSlug
                              ).right;

          // And valid shipping information (if necessary by printing option)
          maybeShippingForms <- redirectOrValidShippingFormOption(
                                  printingOption,
                                  celebrityUrlSlug,
                                  productUrlSlug
                                ).right;

          // Billing form has to be legit. Hand it the shipping form in case of
          // they had entered "billing matches shipping"
          validBillingForm <- redirectOrValidBillingForm(
                                maybeShippingForms._1,
                                celebrityUrlSlug,
                                productUrlSlug
                              ).right
        ) yield {
          forms.withHighQualityPrint(printingOption).save()

          EgraphPurchaseHandler(
            recipientName=validPersonalizeForm.recipientName,
            recipientEmail=validPersonalizeForm.recipientEmail.getOrElse(validBillingForm.email),
            buyerName=validBillingForm.name,
            buyerEmail=validBillingForm.email,
            stripeTokenId=validBillingForm.paymentToken,
            desiredText=validPersonalizeForm.writtenMessageText,
            personalNote=validPersonalizeForm.noteToCelebriity,
            celebrity=celeb,
            product=product
          ).execute()
        }
    }
  }

  private def redirectOrValidShippingFormOption(
    printingOption: PrintingOption,
    celebrityUrlSlug: String,
    productUrlSlug: String
  ): Either[Redirect, (Option[CheckoutShippingForm], Option[CheckoutShippingForm.Valid])] = {
    printingOption match {
      case PrintingOption.HighQualityPrint =>
        val formReader = purchaseFormReaders.forShippingForm
        val shippingForm = formReader.instantiateAgainstReadable(params.asFormReadable)
        val errorsOrValid = shippingForm.errorsOrValidatedForm
        val redirectOrValid = errorsOrValid.left.map { error =>
         redirectCheckoutFormsThroughFlash(celebrityUrlSlug, productUrlSlug)
        }

        redirectOrValid.right.map(valid => (Some(shippingForm), Some(valid)))

      case PrintingOption.DoNotPrint =>
        Right((None, None))
    }
  }

  private def redirectOrValidBillingForm(
    maybeShippingForm: Option[CheckoutShippingForm],
    celebrityUrlSlug: String,
    productUrlSlug: String
  ): Either[Redirect, CheckoutBillingForm.Valid] = {
    val billingFormReader = purchaseFormReaders.forBillingForm(maybeShippingForm)
    val billingForm = billingFormReader.instantiateAgainstReadable(params.asFormReadable)
    val errorsOrValid = billingForm.errorsOrValidatedForm

    errorsOrValid.left.map { _ =>
      redirectCheckoutFormsThroughFlash(celebrityUrlSlug, productUrlSlug)
    }
  }

  private def redirectCheckoutFormsThroughFlash(celebrityUrlSlug: String, productUrlSlug: String): Redirect = {
    // Get readers for the shipping and billing forms
    val readers = List(purchaseFormReaders.forShippingForm, purchaseFormReaders.forBillingForm(None))

    val paramFormReadable = params.asFormReadable
    val formWriteableFlash = flash.asFormWriteable

    for (formReader <- readers) {
      formReader.instantiateAgainstReadable(paramFormReadable).write(formWriteableFlash)
    }

    new Redirect(reverse(getStorefrontCheckout(celebrityUrlSlug, productUrlSlug)).url)
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