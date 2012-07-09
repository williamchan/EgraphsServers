package controllers.website.consumer

import services.http.{SafePlayParams, POSTControllerMethod, CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.mvc.results.Redirect
import services.http.forms.purchase._
import models.enums.{PrintingOption, WrittenMessageRequest}
import PrintingOption.HighQualityPrint
import PrintOptionForm.Params
import controllers.WebsiteControllers
import WebsiteControllers.{getStorefrontPersonalize, getStorefrontCheckout}
import SafePlayParams.Conversions._
import services.Utils

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
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def checkPurchaseField: PurchaseFormChecksFactory

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
  def getStorefrontReview(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod() {
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
                                ).right
      ) yield {
        // Everything looks good for rendering the page!
        val textCelebWillWrite = PurchaseForms.makeTextForCelebToWrite(
          validPersonalizeForm.writtenMessageRequest,
          validPersonalizeForm.writtenMessageText
        )

        val doPrint = forms.highQualityPrint
          .map(printingOption => printingOption == HighQualityPrint)
          .getOrElse(false)

        views.frontend.html.celebrity_storefront_review(
          celebrityName = celeb.publicName.getOrElse("Anonymous"),
          productTitle = product.name,
          celebrityWillWrite = textCelebWillWrite,
          recipientName = validPersonalizeForm.recipientName,
          noteToCelebrity = validPersonalizeForm.noteToCelebriity,
          basePrice = product.price,
          guaranteedDelivery = inventoryBatch.endDate,
          highQualityPrintParamName = Params.HighQualityPrint,
          highQualityPrint = doPrint,
          actionUrl = reverse(postStorefrontReview(celebrityUrlSlug, productUrlSlug)).url
        )
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
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
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
        forms.withHighQualityPrint(validPrintOption).save()

        // TODO: redirect to "Checkout As" screen if not logged in rather than straight to
        // checkout.
        val defaultNextUrl = reverse(getStorefrontCheckout(celebrityUrlSlug, productUrlSlug)).url

        Utils.redirectToClientProvidedTarget(urlIfNoTarget=defaultNextUrl)
      }
    }
  }

}
