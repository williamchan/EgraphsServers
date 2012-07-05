package controllers.website.consumer

import services.http.{SafePlayParams, POSTControllerMethod, CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import services.http.forms.purchase._
import models.enums.{PrintingOption, WrittenMessageRequest}
import PrintingOption.HighQualityPrint
import PrintOptionForm.Params
import controllers.WebsiteControllers
import WebsiteControllers.{getStorefrontCheckout, getStorefrontPersonalize}
import SafePlayParams.Conversions._
import services.Utils
import models.frontend.storefront._
import services.payment.Payment
import models.frontend.storefront.FinalizeBillingViewModel
import models.frontend.storefront.FinalizeViewModel
import models.frontend.storefront.FinalizePersonalizationViewModel
import models.frontend.storefront.FinalizeShippingViewModel

/**
 * Manages GET and POST of the Review page in the purchase flow.
 */
private[consumer] trait StorefrontFinalizeConsumerEndpoints
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData {
  this: Controller =>

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod

  protected def postController: POSTControllerMethod

  protected def purchaseFormFactory: PurchaseFormFactory

  protected def purchaseFormReaders: PurchaseFormReaders

  protected def celebFilters: CelebrityAccountRequestFilters

  protected def checkPurchaseField: PurchaseFormChecksFactory

  protected def payment: Payment

  //
  // Controllers
  //
  /**
   * Controller that GETs the "Review" page in the purchase flow.
   *
   * @param celebrityUrlSlug identifies the celebrity from which the user is purchasing
   * @param productUrlSlug identifies the photo being personalized
   * @return the web page, or a Redirect to earlier forms in the flow if their data
   *         was found to be lacking.
   */
  def getStorefrontFinalize(celebrityUrlSlug: String, productUrlSlug: String): Any = controllerMethod() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      // Get the purchase forms out of the server session
      val forms = purchaseFormFactory.formsForStorefront(celeb.id)

      for (
      // Make sure the product ID in this URL matches the one in the form
        formProductId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;

        // Make sure there's inventory on the product
        inventoryBatch <- forms.nextInventoryBatchOrRedirect(celebrityUrlSlug, product).right;

        // Make sure we've got a valid personalize form in storage
        validPersonalizeForm <- forms.validPersonalizeFormOrRedirectToPersonalizeForm(
                                  celebrityUrlSlug,
                                  productUrlSlug
                                ).right;

      // Make sure we've got valid personalize forms.
        validCheckoutForms <- forms.validCheckoutFormsOrRedirectToCheckout(
                                celebrityUrlSlug,
                                productUrlSlug
                              ).right
      ) yield {
        // Everything looks good for rendering the page!
        val (billing, maybeShipping) = validCheckoutForms

        // Create the checkout viewmodels
        val checkoutUrl = reverse(getStorefrontCheckout(celebrityUrlSlug, productUrlSlug)).url

        val billingViewModel = FinalizeBillingViewModel(
          name=billing.name,
          email=billing.email,
          postalCode=billing.postalCode,
          paymentToken=billing.paymentToken,
          paymentApiKey = payment.publishableKey,
          paymentJsModule = payment.browserModule,
          editUrl=checkoutUrl
        )

        val maybeShippingViewModel = maybeShipping.map { shipping =>
          FinalizeShippingViewModel(
            name=shipping.name,
            email=shipping.email,
            addressLine1 = shipping.addressLine1,
            addressLine2 = shipping.addressLine2,
            city = shipping.city,
            state = shipping.state,
            postalCode = shipping.postalCode,
            editUrl = checkoutUrl
          )
        }

        // Create the personalization viewmodel
        val personalizationViewModel = FinalizePersonalizationViewModel(
          celebName=celeb.publicName.getOrElse("Anonymous"),
          productTitle=product.name,
          recipientName = validPersonalizeForm.recipientName,
          messageText=PurchaseForms.makeTextForCelebToWrite(
            validPersonalizeForm.writtenMessageRequest,
            validPersonalizeForm.writtenMessageText
          ),
          editUrl = reverse(getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug)).url
        )

        // Create the pricing viewmodel
        val priceViewModel = FinalizePriceViewModel(
           base=product.price,
           physicalGood=forms.shippingPrice,
           tax=forms.tax,
           total=forms.total(product.price)
        )

        // Create the final viewmodel
        val finalizeViewModel = FinalizeViewModel(
          billing=billingViewModel,
          shipping=maybeShippingViewModel,
          personalization=personalizationViewModel,
          price=priceViewModel,
          purchaseUrl=reverse(postStorefrontFinalize(celebrityUrlSlug, productUrlSlug)).url
        )

        views.frontend.html.celebrity_storefront_finalize(finalizeViewModel)
      }
    }
  }

  /**
   * Controller for POSTing the Review form in the purchase flow.
   *
   * @param celebrityUrlSlug identifies the celebrity from which the user is purchasing
   * @param productUrlSlug identifies the photo being personalized
   * @return a Redirect to the next step in the purchase flow if successful, otherwise
   *         a Redirect back to the form to handle errors.
   */
  def postStorefrontFinalize(celebrityUrlSlug: String, productUrlSlug: String) = postController() {
    celebFilters.requireCelebrityAndProductUrlSlugs {
      (celeb, product) =>
      // Get the purchase forms for this celeb's storefront out of the server session
        val forms = purchaseFormFactory.formsForStorefront(celeb.id)

        // Validate in a for comprehension
        for (
        // Product ID of the URL has to match the product stored in the session
          productId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;

          // User has to have posted a valid printing option. Which should be impossible to
          // screw up because it was a damned checkbox.
          validPrintOption <- checkPurchaseField(params.getOption(Params.HighQualityPrint))
            .isPrintingOption
            .right
        ) yield {
          // Save this form into the server session
          /*forms.withHighQualityPrint(validPrintOption).save()

          // TODO: redirect to "Checkout As" screen if not logged in rather than straight to
          // checkout.
          val defaultNextUrl = reverse(getStorefrontCheckout(celebrityUrlSlug, productUrlSlug)).url

          Utils.redirectToClientProvidedTarget(urlIfNoTarget = defaultNextUrl)*/
        }
    }
  }

  //
  // Private members
  //
}