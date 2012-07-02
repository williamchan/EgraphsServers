package controllers.website.consumer

import services.http.{SafePlayParams, POSTControllerMethod, CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import services.http.forms.purchase._
import models.enums.WrittenMessageRequest
import PrintOptionForm.Params
import SafePlayParams.Conversions._

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

  def getStorefrontCheckout(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod() {
    celebFilters.requireCelebrityAndProductUrlSlugs {
      (celeb, product) =>
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
                                  ).right
        ) yield {

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
      case WrittenMessageRequest.SignatureOnly => "His signature only"
      case WrittenMessageRequest.CelebrityChoosesMessage => "Whatever he wants"
      case WrittenMessageRequest.SpecificMessage => messageText.getOrElse("")
    }
  }
}