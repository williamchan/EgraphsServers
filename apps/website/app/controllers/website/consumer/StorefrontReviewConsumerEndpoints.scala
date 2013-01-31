package controllers.website.consumer

import services.http.{POSTControllerMethod, ControllerMethod}
import play.api.mvc.Controller
import services.mvc.{StorefrontBreadcrumbData, ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import services.http.forms.purchase._
import play.api.data.Form
import play.api.data.Forms.{text, single, optional}
import models.enums.PrintingOption
import PrintingOption.HighQualityPrint
import PrintOptionForm.Params
import controllers.routes.WebsiteControllers.getStorefrontCheckout
import services.Utils
import services.blobs.AccessPolicy
import services.http.filters.HttpFilters
import play.api.mvc.Action
import controllers.routes.WebsiteControllers.{getStorefrontReview => reverseGetStorefrontReview}
import org.joda.time.{DateMidnight, DateTimeConstants}
import models.Order

/**
 * Manages GET and POST of the Review page in the purchase flow.
 */
private[consumer] trait StorefrontReviewConsumerEndpoints
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
  protected def breadcrumbData: StorefrontBreadcrumbData

  //
  // Controllers
  //
  /**
   * Controller that GETs the "Review" page in the purchase flow.
   *
   * @param celebrityUrlSlug identifies the celebrity from which the user is purchasing
   * @param productUrlSlug identifies the photo being personalized
   * @return the web page, or a Redirect to earlier forms in the flow if their data
   *   was found to be lacking.
   */
  def getStorefrontReview(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod.withForm() 
  { implicit authToken =>

    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>
        // Get the purchase forms out of the server session
        val forms = purchaseFormFactory.formsForStorefront(celeb.id)(request.session)
  
        val redirectOrOk = for (
          // Make sure the product ID in this URL matches the one in the form
          formProductId <- forms.redirectToChoosePhotoOrMatchingProductId(celeb, product).right;
  
          // Make sure there's inventory on the product
          inventoryBatch <- forms.redirectOrNextInventoryBatch(celebrityUrlSlug, product).right;
  
          // Make sure we've got a valid personalize form in storage
          validPersonalizeForm <- forms.redirectToPersonalizeFormOrValidPersonalizeForm(
                                    celebrityUrlSlug,
                                    productUrlSlug
                                  ).right
        ) yield {
          // Everything looks good for rendering the page!
          val textCelebWillWrite = PurchaseForms.makeTextForCelebToWrite(
            validPersonalizeForm.writtenMessageRequest,
            validPersonalizeForm.writtenMessageText,
            celebrityGender = celeb.gender
          )
  
          val doPrint = forms.highQualityPrint
            .map(printingOption => printingOption == HighQualityPrint)
            .getOrElse(false)
  
          implicit def crumbs = breadcrumbData.crumbsForRequest(celeb.id, celebrityUrlSlug, Some(productUrlSlug))(request)
  
          Ok(views.html.frontend.celebrity_storefront_review(
            celebrityName = celeb.publicName,
            productTitle = product.name,
            celebrityWillWrite = textCelebWillWrite,
            recipientName = validPersonalizeForm.recipientName,
            noteToCelebrity = validPersonalizeForm.noteToCelebrity,
            basePrice = product.price,
            guaranteedDelivery = Order.expectedDeliveryDate(celeb),
            highQualityPrintParamName = Params.HighQualityPrint,
            highQualityPrint = doPrint,
            actionUrl = controllers.routes.WebsiteControllers.postStorefrontReview(celebrityUrlSlug, productUrlSlug).url,
            productPreviewUrl=product.photoAtPurchasePreviewSize.getSaved(AccessPolicy.Public).url,
            orientation=product.frame.previewCssClass
          ))
        }
        
        redirectOrOk.merge
      }
    }
  }

  /**
   * Controller for POSTing the Review form in the purchase flow.
   *
   * @param celebrityUrlSlug identifies the celebrity from which the user is purchasing
   * @param productUrlSlug identifies the photo being personalized
   * @return a Redirect to the next step in the purchase flow if successful, otherwise
   *    a Redirect back to the form to handle errors.
   */
  def postStorefrontReview(celebrityUrlSlug: String, productUrlSlug: String) = postController() {
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request => 
        // Get the purchase forms for this celeb's storefront out of the server session
        val forms = purchaseFormFactory.formsForStorefront(celeb.id)(request.session)
        
        val maybePrintingOption = Form(single(Params.HighQualityPrint -> optional(text))).bindFromRequest.get
        
        // Validate in a for comprehension
        val failureOrSuccessRedirect = for (
          // Product ID of the URL has to match the product stored in the session
          productId <- forms.redirectToChoosePhotoOrMatchingProductId(celeb, product).right;
  
          // User has to have posted a valid printing option. Which should be impossible to
          // screw up because it was a damned checkbox. But if they did screw it up they
          // get a redirect.
          validPrintOption <- checkPurchaseField(maybePrintingOption)
                                .isPrintingOption
                                .left.map { formError => 
                                  Redirect(reverseGetStorefrontReview(celebrityUrlSlug, productUrlSlug))
                                }
                                .right
        ) yield {
          // Save this form into the server session
          forms.withHighQualityPrint(validPrintOption).save()
  
          // TODO: redirect to "Checkout As" screen if not logged in rather than straight to
          // checkout.
          val defaultNextUrl = getStorefrontCheckout(celebrityUrlSlug, productUrlSlug).url
  
          Utils.redirectToClientProvidedTarget(urlIfNoTarget=defaultNextUrl)
        }
        
        failureOrSuccessRedirect.merge
      }
    }
  }

}
