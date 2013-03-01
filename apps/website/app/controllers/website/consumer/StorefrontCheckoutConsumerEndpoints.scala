package controllers.website.consumer

import services.http.{POSTControllerMethod, ControllerMethod}
import play.api.mvc.Controller
import services.mvc.{StorefrontBreadcrumbData, ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import services.http.forms.purchase._
import models.CouponStore
import models.enums.PrintingOption
import services.http.forms.Form.Conversions._
import services.mvc.FormConversions.{checkoutBillingFormToViewConverter, checkoutShippingFormToViewConverter}
import models.frontend.storefront.{CheckoutFormView, CheckoutOrderSummary, CheckoutBillingInfoView, CheckoutShippingAddressFormView}
import services.payment.Payment
import play.api.mvc.Results.Redirect
import play.api.mvc.Result
import play.api.mvc.Request
import controllers.{routes, WebsiteControllers}
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
  protected def couponStore: CouponStore

  //
  // Controllers
  //
  def getStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = Action { req =>
    Redirect(routes.WebsiteControllers.getPersonalize(celebrityUrlSlug))
  }

  def postStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = Action { req => NotFound }




  /** Controller that GETs the checkout page, or Redirects to another form if there was
   *  insufficient data in the user's session to present the checkout page.
   *
   *  @param celebrityUrlSlug identifies the celebrity whose product is being checked out.
   *  @param productUrlSlug identifies the product being checked out.
   **/
  @deprecated("Transitioned to new Checkout", "02/27/2013")
  def _getStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = {
    controllerMethod.withForm() { implicit token =>
      httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
        Action { implicit request =>
          val forms = purchaseFormFactory.formsForStorefront(celeb.id)(request.session)
    
          val results = for (
            // Make sure the product ID matches
            formProductId <- forms.redirectToChoosePhotoOrMatchingProductId(celeb, product).right;
    
            // Make sure there's inventory
            inventoryBatch <- forms.redirectOrNextInventoryBatch(celebrityUrlSlug, product).right;
    
            // Make sure we've got a personalize form in storage, or redirect to personalize
            validPersonalizeForm <- forms.redirectToPersonalizeFormOrValidPersonalizeForm(
                                      celebrityUrlSlug,
                                      productUrlSlug
                                    ).right;
    
            // Make sure we've got a high-quality print option from the Review page, or redirect to it
            highQualityPrint <- forms.redirectToReviewFormOrPrintingOption(
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

              case other => throw new IllegalStateException("models.enums.PrintingOption cannot have value = " + other)
            }
    
            // View for the billing form.
            val billingFormView = forms.billingForm(Some(request.flash)).map { billing =>
              billing.asCheckoutPageView
            }.getOrElse {
              defaultBillingView
            }
            
            val maybeCoupon = forms.coupon
            val subtotal = forms.subtotal(product.price)
            val discount = forms.discount(subtotal = subtotal, maybeCoupon)
            
            val orderSummary = CheckoutOrderSummary(
              celebrityName=celeb.publicName,
              productName=product.name,
              recipientName=validPersonalizeForm.recipientName,
              messageText=PurchaseForms.makeTextForCelebToWrite(
                validPersonalizeForm.writtenMessageRequest,
                validPersonalizeForm.writtenMessageText,
                celebrityGender = celeb.gender
              ),
              basePrice=product.price,
              shipping=forms.shippingPrice,
              tax=forms.tax,
              discount=discount,
              total=forms.total(subtotal = subtotal, discount = discount)
            )
    
            // Collect both the shipping form and billing form into a single viewmodel
            val checkoutFormView = CheckoutFormView(
              actionUrl=controllers.routes.WebsiteControllers.postStorefrontCheckout(celebrityUrlSlug, productUrlSlug).url,
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
          }
  
          results.merge
        }
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
  @deprecated("Transitioned to new Checkout", "02/27/2013")
  def _postStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = postController() {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        val forms = purchaseFormFactory.formsForStorefront(celeb.id)(request.session)

        // For-comprehend over a bunch of validations that produces success or failure redirects
        val redirects = for (
          // Product ID in the url must match the product being ordered, or redirect to photo
          productId <- forms.redirectToChoosePhotoOrMatchingProductId(celeb, product).right;

          // There must be remaining inventory on the product
          inventoryBatch <- forms.redirectOrNextInventoryBatch(celebrityUrlSlug, product).right;

          // Gotta have a valid personalize form in storage
          validPersonalizeForm <- forms.redirectToPersonalizeFormOrValidPersonalizeForm(
                                    celebrityUrlSlug,
                                    productUrlSlug
                                  ).right;

          // And a printing option from the review page
          printingOption <- forms.redirectToReviewFormOrPrintingOption(
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
            celebrityUrlSlug,
            productUrlSlug
          ) 
          
          Redirect(finalizeRedirect)
        }
        
        redirects.merge
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

      case other => throw new IllegalStateException("models.enums.PrintingOption cannot have value = " + other)
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
      AddressLine1,
      AddressLine2,
      City,
      State,
      PostalCode,
      BillingIsSame
    )
  }
}
