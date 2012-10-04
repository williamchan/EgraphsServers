package controllers.website.consumer

import services.http.{SafePlayParams, POSTControllerMethod, ControllerMethod}
import services.http.filters.HttpFilters
import play.api.mvc.Controller
import services.mvc.{StorefrontBreadcrumbData, ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import services.http.forms.purchase._
import models.enums.PrintingOption
import controllers.WebsiteControllers
import controllers.routes.WebsiteControllers.{getStorefrontCheckout, getStorefrontPersonalize}
import models.frontend.storefront._
import services.payment.Payment
import models.frontend.storefront.FinalizeBillingViewModel
import models.frontend.storefront.FinalizeViewModel
import models.frontend.storefront.FinalizePersonalizationViewModel
import models.frontend.storefront.FinalizeShippingViewModel
import controllers.website.EgraphPurchaseHandler
import services.db.{TransactionSerializable, DBSession}
import services.http.forms.purchase.PurchaseForms.AllPurchaseForms
import models.Celebrity
import services.blobs.AccessPolicy
import play.api.mvc.Action

/**
 * Manages GET and POST of the Finalize page in the purchase flow.
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
  protected def formReaders: FormReaders
  protected def httpFilters: HttpFilters
  protected def checkPurchaseField: PurchaseFormChecksFactory
  protected def payment: Payment
  protected def dbSession: DBSession
  protected def breadcrumbData: StorefrontBreadcrumbData

  //
  // Controllers
  //
  /**
   * Controller that GETs the "Finalize" page in the purchase flow.
   *
   * @param celebrityUrlSlug identifies the celebrity from which the user is purchasing
   * @param productUrlSlug identifies the photo being personalized
   * @return the web page, or a Redirect to earlier forms in the flow if their data
   *         was found to be lacking.
   */
  def getStorefrontFinalize(celebrityUrlSlug: String, productUrlSlug: String): Action[_] = controllerMethod() {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        // Get the purchase forms out of the server session
        val forms = purchaseFormFactory.formsForStorefront(celeb.id)(request.session)
  
        val results = for (allPurchaseForms <- forms.allPurchaseFormsOrRedirect(celeb, product).right) yield {
          // Everything looks good for rendering the page! Unpack the purchase data.
          val AllPurchaseForms(
            formProductId,
            inventoryBatch,
            validPersonalizeForm,
            billing,
            maybeShipping
          ) = allPurchaseForms
  
          // Create the checkout viewmodels
          val checkoutUrl = getStorefrontCheckout(celebrityUrlSlug, productUrlSlug).url
  
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
            celebName=celeb.publicName,
            productTitle=product.name,
            recipientName = validPersonalizeForm.recipientName,
            messageText=PurchaseForms.makeTextForCelebToWrite(
              validPersonalizeForm.writtenMessageRequest,
              validPersonalizeForm.writtenMessageText
            ),
            editUrl = getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug).url
          )
  
          // Create the pricing viewmodel
          val priceViewModel = FinalizePriceViewModel(
             base=product.price,
             physicalGood=forms.shippingPrice,
             tax=forms.tax,
             total=forms.total(basePrice = product.price)
          )
  
          // Create the final viewmodel
          val finalizeViewModel = FinalizeViewModel(
            billing=billingViewModel,
            shipping=maybeShippingViewModel,
            personalization=personalizationViewModel,
            price=priceViewModel,
            purchaseUrl=controllers.routes.WebsiteControllers.postStorefrontFinalize(celebrityUrlSlug, productUrlSlug).url
          )
  
          implicit def crumbs = breadcrumbData.crumbsForRequest(celeb.id, celebrityUrlSlug, Some(productUrlSlug))(request)
  
          Ok(views.html.frontend.celebrity_storefront_finalize(
            finalizeViewModel,
            productPreviewUrl=product.photoAtPurchasePreviewSize.getSaved(AccessPolicy.Public).url,
            orientation=product.frame.previewCssClass
          ))
        }
        
        results.fold(failure => failure, ok => ok)
      }
    }
  }

  /**
   * Controller for POSTing the Finalize form in the purchase flow.
   *
   * @param celebrityUrlSlug identifies the celebrity from which the user is purchasing
   * @param productUrlSlug identifies the photo being personalized
   * @return a Redirect to the order complete page if successful, otherwise
   *         a Redirect back to the form to handle errors.
   */
  def postStorefrontFinalize(celebrityUrlSlug: String, productUrlSlug: String) = postController(openDatabase=false) {
    Action { implicit request =>
      // Get all the sweet, sweet purchase form data in a database transaction. We end up with a weird 
      // Either[Result, Either[Result, (The purchase data)], but we'll unpack them later. If you're feeling
      // Brave try to make this into a for comprehension inside of dbSession.connected.
      val redirectOrRedirectOrPurchaseData = dbSession.connected(TransactionSerializable) {
        httpFilters.requireCelebrityAndProductUrlSlugs.asOperationResult(celebrityUrlSlug, productUrlSlug, request.session) { 
          (celeb, product) =>
            val forms = purchaseFormFactory.formsForStorefront(celeb.id)(request.session)
            for (formData <- forms.allPurchaseFormsOrRedirect(celeb, product).right) yield {
              (celeb, product, formData, forms)
            }
        }
      }
      
      val failureOrSuccessRedirects = for (
        redirectOrPurchaseData <- redirectOrRedirectOrPurchaseData.right;
        purchaseData <- redirectOrPurchaseData.right
      ) yield {
        val (celeb, product, shippingForms, forms) = purchaseData 
        val AllPurchaseForms(productId, inventoryBatch, personalization, billing, shipping) = shippingForms

        EgraphPurchaseHandler(
          recipientName=personalization.recipientName,
          recipientEmail=personalization.recipientEmail.getOrElse(billing.email),
          buyerName=billing.name,
          buyerEmail=billing.email,
          stripeTokenId=billing.paymentToken,
          desiredText=personalization.writtenMessageText,
          personalNote=personalization.noteToCelebriity,
          celebrity=celeb,
          product=product,
          totalAmountPaid=forms.total(basePrice = product.price),
          billingPostalCode=billing.postalCode,
          flash=request.flash,
          printingOption=forms.highQualityPrint.getOrElse(PrintingOption.DoNotPrint),
          shippingForm=shipping,
          writtenMessageRequest=personalization.writtenMessageRequest
        ).execute()        
      }
      
      failureOrSuccessRedirects.fold(
        failureRedirect => failureRedirect, 
        successRedirect => successRedirect
      )
    }  
  }
}
