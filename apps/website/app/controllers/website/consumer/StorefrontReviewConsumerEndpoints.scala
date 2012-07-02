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
import WebsiteControllers.getStorefrontPersonalize
import SafePlayParams.Conversions._

/**
 * Endpoint for serving up the Choose Photo page
 */
private[consumer] trait StorefrontReviewConsumerEndpoints
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData {
  this: Controller =>


  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod
  protected def purchaseFormFactory: PurchaseFormFactory
  protected def purchaseFormReaders: PurchaseFormReaders
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def checkPurchaseField: PurchaseFormChecksFactory

  def getStorefrontReview(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      val forms = purchaseFormFactory.formsForStorefront(celeb.id)

      for (
        // Make sure the product ID matches
        formProductId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;

        // Make sure there's inventory
        inventoryBatch <- forms.nextInventoryBatchOrRedirect(celebrityUrlSlug, product).right;

        // Make sure we've got a personalize form in storage, or redirect to personalize
        personalizeForm <- forms.personalizeFormOrRedirectToPersonalizeForm(
                             celebrityUrlSlug,
                             productUrlSlug
                           ).right;

        // Get the validated version of the personalize form so we don't have to do a bunch of .gets
        validPersonalizeForm <- personalizeForm.errorsOrValidatedForm.left.map { formError =>
                                  val action = reverse(getStorefrontPersonalize(
                                    celebrityUrlSlug,
                                    productUrlSlug
                                  ))

                                  new Redirect(action.url)
                                }.right
      ) yield {
        val textCelebWillWrite = makeTextForCelebToWrite(
          validPersonalizeForm.writtenMessageRequest,
          validPersonalizeForm.writtenMessageMaybe
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

  def postStorefrontReview(celebrityUrlSlug: String, productUrlSlug: String) = postController() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      val forms = purchaseFormFactory.formsForStorefront(celeb.id)

      for (
        productId <- forms.matchProductIdOrRedirectToChoosePhoto(celeb, product).right;
        validPrintOption <- checkPurchaseField(params.getOption(Params.HighQualityPrint))
                              .isPrintingOption
                              .right
      ) yield {
        forms.withHighQualityPrint(validPrintOption).save()

        // TODO: redirect to "Checkout As" screen if not logged in

      }
    }
  }

  private def makeTextForCelebToWrite(messageRequest: WrittenMessageRequest, messageText: Option[String])
  : String = {
    messageRequest match {
      // TODO: Make these strings respond to gender
      case WrittenMessageRequest.SignatureOnly => "His signature only"
      case WrittenMessageRequest.CelebrityChoosesMessage => "Whatever he wants"
      case WrittenMessageRequest.SpecificMessage => messageText.getOrElse("")
    }
  }
}