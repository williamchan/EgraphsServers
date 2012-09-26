package controllers.website.consumer

import services.mvc.{StorefrontBreadcrumbData, ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.api.mvc.Controller
import services.http.{POSTControllerMethod, ControllerMethod}
import services.http.forms.purchase.{PurchaseFormChecks, FormReaders, PersonalizeForm, PurchaseFormFactory}
import services.mvc.FormConversions.personalizeFormToView
import controllers.WebsiteControllers
import models.frontend.storefront.{PersonalizeForm => PersonalizeFormView, StorefrontOrderSummary}
import services.Utils
import services.http.forms.Form.Conversions._
import services.blobs.AccessPolicy
import services.http.filters.HttpFilters
import play.api.mvc.Action

/**
 * Manages GET and POST of the egraph purchase personalization form.
 */
trait StorefrontPersonalizeConsumerEndpoints
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData
{ this: Controller =>

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod
  protected def purchaseFormFactory: PurchaseFormFactory
  protected def formReaders: FormReaders
  protected def httpFilters: HttpFilters
  protected def breadcrumbData: StorefrontBreadcrumbData

  //
  // Controllers
  //
  /**
   * Gets the personalization form page in the purchase flow.
   *
   * @param celebrityUrlSlug identifies the celebrity from which the user is purchasing
   * @param productUrlSlug identifies the photo being personalized
   * @return the web page, or a redirect if the product was not found in the user's server-
   *     side session cache.
   */
  def getStorefrontPersonalize(celebrityUrlSlug: String, productUrlSlug: String):Any = controllerMethod() {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { request =>
        // Get the purchase forms specific to this celebrity's storefront.
        val forms = purchaseFormFactory.formsForStorefront(celeb.id)
  
        val redirectOrOk = for (
          // Make sure the submitted product ID matches the one in the session, otherwise redirect
          // to the Choose Photo screen
          formProductId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;
  
          // Get the next open inventory batch that an order would go into, and if there's no
          // inventory then redirect to the insufficient inventory page.
          nextInventoryBatch <- forms.nextInventoryBatchOrRedirect(celebrityUrlSlug, product).right
        ) yield {
          val actionTarget = controllers.routes.WebsiteControllers.postStorefrontPersonalize(celebrityUrlSlug, productUrlSlug).url
  
          // Try to get the forms out of the flash primarily and server-session secondarily.
          val formViewOption = forms.personalizeForm(Some(request.flash)).map { form =>
            form.asPersonalizeFormView(actionTarget)
          }
  
          // If neither flash nor server had the forms, render a default, empty form.
          val formView = formViewOption.getOrElse {
            import PersonalizeForm.Params
  
            PersonalizeFormView.empty(
              actionUrl=actionTarget,
              isGiftParam=Params.IsGift,
              recipientNameParam=Params.RecipientName,
              recipientEmailParam=Params.RecipientEmail,
              messageOptionParam = Params.WrittenMessageRequest,
              messageTextParam = Params.WrittenMessageRequestText,
              noteToCelebrityParam = Params.NoteToCelebrity
            )
          }
  
          val orderSummary = StorefrontOrderSummary(
            celebrityName = celeb.publicName,
            productName = product.name,
            subtotal = product.price,
            shipping = forms.shippingPrice,
            tax = forms.tax,
            total = forms.total(basePrice=product.price)
          )
  
          implicit def crumbs = breadcrumbData.crumbsForRequest(celeb.id, celebrityUrlSlug, Some(productUrlSlug))(request)
  
          Ok(views.html.frontend.celebrity_storefront_personalize(
            form=formView,
            guaranteedDelivery=nextInventoryBatch.getExpectedDate,
            writtenMessageCharacterLimit=PurchaseFormChecks.maxWrittenMessageChars,
            messageToCelebrityCharacterLimit=PurchaseFormChecks.maxNoteToCelebChars,
            orderSummary=orderSummary,
            productPreviewUrl=product.photoAtPurchasePreviewSize.getSaved(AccessPolicy.Public).url,
            orientation=product.frame.previewCssClass
          ))
        }
        
        redirectOrOk.fold(failure => failure, success => success)
      }
    }
  }

  /**
   * Accepts form POSTs of the personalization form in the purchase flow.
   *
   * @param celebrityUrlSlug identifies the celebrity from which the user is purchasing
   * @param productUrlSlug identifies the photo being personalized
   * @return a Redirect to the Review page if successful, or back to this form page if unsuccessful.
   */
  def postStorefrontPersonalize(celebrityUrlSlug: String, productUrlSlug: String) = postController() {    
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { request =>
        import controllers.routes.WebsiteControllers.getStorefrontReview
        
        // Get the set of purchase forms from the server session
        val purchaseForms = purchaseFormFactory.formsForStorefront(celeb.id)
  
        // Read the Personalize form out of the parameters
        val formReader = formReaders.forPersonalizeForm
        val form = formReader.instantiateAgainstReadable(request.asFormReadable)
  
        // Comprehend over a bunch of validation checks
        val failureOrSuccessRedirect = for (
          // Product ID in the url had to match the product currently being ordered,
          // or redirect to choose photo.
          productId <- purchaseForms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;
  
          // Form had to be valid, or redirect back to the form page.
          validated <- form.errorsOrValidatedForm.left.map { error =>
                         val action = reverse(getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug))
  
                         form.redirectThroughFlash(action.url)
                       }.right
        ) yield {
          // Everything looked good. Save the form into the cache and move on with life.
          purchaseForms.withForm(form).save()
  
          val defaultNextUrl = reverse(getStorefrontReview(celebrityUrlSlug, productUrlSlug)).url
  
          Utils.redirectToClientProvidedTarget(urlIfNoTarget=defaultNextUrl)
        }
        
        failureOrSuccessRedirect.fold(failure => failure, success => success)
      }
    }
  }
}
