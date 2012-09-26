package controllers.website.consumer

import services.http.{POSTControllerMethod, ControllerMethod}
import play.api.mvc.Controller
import services.mvc.{StorefrontBreadcrumbData, ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import services.http.forms.purchase._
import models.enums.PrintingOption
import services.http.forms.Form.Conversions._
import services.mvc.FormConversions.{checkoutBillingFormToViewConverter, checkoutShippingFormToViewConverter}
import models.frontend.storefront.{CheckoutFormView, CheckoutOrderSummary, CheckoutBillingInfoView, CheckoutShippingAddressFormView}
import services.payment.Payment
import play.api.mvc.Results.Redirect
import play.api.mvc.Result
import play.api.mvc.Request
import controllers.WebsiteControllers
import services.blobs.AccessPolicy
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.mvc.AnyContent

/**
 * Endpoint for serving up the Checkout form
 */
private[consumer] trait StorefrontCheckoutConsumerEndpoints
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData {
  this: Controller =>

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  
  protected def purchaseFormFactory: PurchaseFormFactory
  protected def formReaders: FormReaders  
  protected def checkPurchaseField: PurchaseFormChecksFactory
  protected def payment: Payment
  protected def breadcrumbData: StorefrontBreadcrumbData

  //
  // Controllers
  //
  /** Controller that GETs the checkout page, or Redirects to another form if there was
   *  insufficient data in the user's session to present the checkout page.
   *
   *  @param celebrityUrlSlug identifies the celebrity whose product is being checked out.
   *  @param productUrlSlug identifies the product being checked out.
   **/
  def getStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod() {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
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
          highQualityPrint <- forms.printingOptionOrRedirectToReviewForm(
                                celebrityUrlSlug,
                                productUrlSlug
                              ).right
        ) yield {
          // View for the shipping form -- only show it if ordering a print.
          val maybeShippingFormView = highQualityPrint match {
            case PrintingOption.HighQualityPrint =>
              val restoredOrDefaultShipping = forms.shippingForm(Some(request.flash)).map {
                shipping => shipping.asCheckoutPageView
              }.getOrElse {
                defaultShippingView
              }
  
              Some(restoredOrDefaultShipping)
  
            case PrintingOption.DoNotPrint =>
              None
          }
  
          // View for the billing form.
          val billingFormView = forms.billingForm(Some(request.flash)).map { billing =>
            billing.asCheckoutPageView
          }.getOrElse {
            defaultBillingView
          }
  
          val orderSummary = CheckoutOrderSummary(
            celebrityName=celeb.publicName,
            productName=product.name,
            recipientName=validPersonalizeForm.recipientName,
            messageText=PurchaseForms.makeTextForCelebToWrite(
              validPersonalizeForm.writtenMessageRequest,
              validPersonalizeForm.writtenMessageText
            ),
            basePrice=product.price,
            shipping=forms.shippingPrice,
            tax=forms.tax,
            total=forms.total(basePrice = product.price)
          )
  
          // Collect both the shipping form and billing form into a single viewmodel
          val checkoutFormView = CheckoutFormView(
            actionUrl=reverse(postStorefrontCheckout(celebrityUrlSlug, productUrlSlug)).url,
            billing=billingFormView,
            shipping=maybeShippingFormView
          )
  
          implicit def crumbs = breadcrumbData.crumbsForRequest(celeb.id, celebrityUrlSlug, Some(productUrlSlug))
  
          // Now baby you've got a stew goin!
          Ok(
            views.html.frontend.celebrity_storefront_checkout(
              form=checkoutFormView,
              summary=orderSummary,
              paymentJsModule=payment.browserModule,
              paymentPublicKey=payment.publishableKey,
              productPreviewUrl=product.photoAtPurchasePreviewSize.getSaved(AccessPolicy.Public).url,
              orientation=product.frame.previewCssClass
            )
          )
        }.fold(redirect => redirect, ok => ok)
      }      
    }
  }

  /**
   * Controller that POSTs the form served by
   * [[controllers.website.consumer.StorefrontCheckoutConsumerEndpoints.getStorefrontCheckout()]]
   *
   * @param celebrityUrlSlug identifies the celebrity being purchased from
   * @param productUrlSlug identifies the product being purchased
   * @return a redirect either to the finalize order page or back to this form to fix errors.
   */
  def postStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = postController() {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        val forms = purchaseFormFactory.formsForStorefront(celeb.id)

        // For-comprehend over a bunch of validations that produces success or failure redirects
        val redirects = for (
          // Product ID in the url must match the product being ordered, or redirect to photo
          productId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;

          // There must be remaining inventory on the product
          inventoryBatch <- forms.nextInventoryBatchOrRedirect(celebrityUrlSlug, product).right;

          // Gotta have a valid personalize form in storage
          validPersonalizeForm <- forms.validPersonalizeFormOrRedirectToPersonalizeForm(
                                    celebrityUrlSlug,
                                    productUrlSlug
                                  ).right;

          // And a printing option from the review page
          printingOption <- forms.printingOptionOrRedirectToReviewForm(
                                celebrityUrlSlug,
                                productUrlSlug
                              ).right;

          // And valid shipping information (if necessary given the printing option)
          maybeShippingForms <- redirectOrValidShippingFormOption(
                                  printingOption,
                                  celebrityUrlSlug,
                                  productUrlSlug
                                ).right;

          // Billing form has to be legit. Hand it the shipping form in case of
          // they had entered "billing matches shipping"
          billingForms <- redirectOrValidBillingForms(
                                 maybeShippingForms._1,
                                 celebrityUrlSlug,
                                 productUrlSlug
                               ).right
        ) yield {
          val billingForm = billingForms._1
          val maybeShippingForm = maybeShippingForms._1

          // Write the shipping form into the session if it was there
          val formsWithShippingForm = maybeShippingForm.map( shipping => forms.withForm(shipping))

          // Write the billing form if it was there and save.
          formsWithShippingForm.getOrElse(forms).withForm(billingForm).save()

          // Redirect to Finalize screen.
          val finalizeRedirect = controllers.routes.WebsiteControllers.getStorefrontFinalize(
            celebrityUrlSlug, productUrlSlug
          ) 
          
          Redirect(finalizeRedirect)
        }
        
        redirects.fold(failureRedirect => failureRedirect, successRedirect => successRedirect)
      }
    }
  }

  //
  // Private members
  //
  private def redirectOrValidShippingFormOption(
    printingOption: PrintingOption,
    celebrityUrlSlug: String,
    productUrlSlug: String
  )(
    implicit request: Request[AnyContent]
  ): Either[Result, (Option[CheckoutShippingForm], Option[CheckoutShippingForm.Valid])] = 
  {
    printingOption match {
      case PrintingOption.HighQualityPrint =>
        val formReader = formReaders.forShippingForm
        val shippingForm = formReader.instantiateAgainstReadable(request.asFormReadable)
        val errorsOrValid = shippingForm.errorsOrValidatedForm
        val redirectOrValid = errorsOrValid.left.map { error =>
         redirectCheckoutFormsThroughFlash(celebrityUrlSlug, productUrlSlug)
        }

        redirectOrValid.right.map(valid => (Some(shippingForm), Some(valid)))

      case PrintingOption.DoNotPrint =>
        Right((None, None))
    }
  }

  private def redirectOrValidBillingForms(
    maybeShippingForm: Option[CheckoutShippingForm],
    celebrityUrlSlug: String,
    productUrlSlug: String
  )(
    implicit request: Request[AnyContent]
  ): Either[Result, (CheckoutBillingForm, CheckoutBillingForm.Valid)] = 
  {
    val billingFormReader = formReaders.forBillingForm(maybeShippingForm)
    val billingForm = billingFormReader.instantiateAgainstReadable(request.asFormReadable)
    val errorsOrValid = billingForm.errorsOrValidatedForm

    val redirectOrValid = errorsOrValid.left.map { _ =>
      redirectCheckoutFormsThroughFlash(celebrityUrlSlug, productUrlSlug)
    }

    redirectOrValid.right.map(validForm => (billingForm, validForm))
  }

  private def redirectCheckoutFormsThroughFlash(
    celebrityUrlSlug: String, 
    productUrlSlug: String
  )(
    implicit request: Request[AnyContent]
  ): Result = 
 {
    // Get readers for the shipping and billing forms
    val readers = List(formReaders.forShippingForm, formReaders.forBillingForm(None))

    val paramFormReadable = request.asFormReadable
    val formWriteableFlash = request.flash.asFormWriteable

    for (formReader <- readers) {
      formReader.instantiateAgainstReadable(paramFormReadable).write(formWriteableFlash)
    }

    Redirect(controllers.routes.WebsiteControllers.getStorefrontCheckout(celebrityUrlSlug, productUrlSlug))
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
