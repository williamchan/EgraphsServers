package services.http.forms.purchase

import services.http.{ServerSessionFactory, ServerSession}
import com.google.inject.Inject
import play.mvc.results.Redirect
import models.{InventoryBatch, Celebrity, Product}
import controllers.WebsiteControllers.{getStorefrontPersonalize, getStorefrontReview, reverse, getStorefrontChoosePhotoTiled, getStorefrontCheckout}
import org.joda.money.{CurrencyUnit, Money}
import models.enums.{WrittenMessageRequest, PrintingOption}
import services.http.forms.{Form, ReadsForm}
import play.mvc.Scope.Flash
import services.http.forms.purchase.PurchaseForms.AllShippingForms

/**
 * Represents the cache-persisted forms in the purchase flow.
 *
 * See the controllers in [[controllers.website.consumer]] for usage.
 *
 * @param formReaders readers for grabbing the different forms out of the session
 *     or flash when nevessary.
 * @param storefrontSession a `ServerSession` namespaced to a particular celebrity's storefront.
 */
class PurchaseForms @Inject()(
  formReaders: PurchaseFormReaders,
  storefrontSession: ServerSession
) {
  import services.http.forms.Form.Conversions._

  import PurchaseForms.Key

  /** The  ID of the [[models.Product]] being purchased from the storefront */
  def productId: Option[Long] = {
    storefrontSession[Long](Key.ProductId)
  }

  /**
   * Retrieves all the valid purchase form data for this product purchase on the right.
   * If the data wasn't available then it returns a redirect to the form page that
   * will provide the data on the left.
   *
   * @param celeb the celebrity being purchased from
   * @param product the product being purchasedf
   * @return either all the data on the right, or a Redirect to the page
   *   to get the data on the left.
   */
  def allPurchaseFormsOrRedirect(celeb: Celebrity, product: Product) = {
    val celebrityUrlSlug = celeb.urlSlug.getOrElse("Anonymous")
    for (
      // Make sure the product ID in this URL matches the one in the form
      productId <- matchProductIdOrRedirectToChoosePhoto(celeb, product).right;
  
      // Make sure there's inventory on the product
      inventoryBatch <- nextInventoryBatchOrRedirect(celebrityUrlSlug, product).right;
  
      // Make sure we've got a valid personalize form in storage
      validPersonalizeForm <- validPersonalizeFormOrRedirectToPersonalizeForm(
        celebrityUrlSlug,
        product.urlSlug
      ).right;
  
      // Make sure we've got valid personalize forms.
      validCheckoutForms <- validCheckoutFormsOrRedirectToCheckout(
        celebrityUrlSlug,
        product.urlSlug
      ).right
    ) yield {
      val (validBilling, validShipping) = validCheckoutForms

      AllShippingForms(productId, inventoryBatch, validPersonalizeForm, validBilling, validShipping)
    }
  }

  /**
   * Returns a copy of this object equipped with the new productId.
   */
  def withProductId(productId: Long): PurchaseForms = {
    this.withSession(storefrontSession.setting(Key.ProductId -> productId))
  }

  /**
   * The current cost of shipping the physical print (or possibly for the full physical
   * print). This will be None if no physical print has been ordered.
   **/
  def shippingPrice: Option[Money] = {
    None
  }

  /**
   * The current tax cost for the purchase. None if not applicable.
   */
  def tax: Option[Money] = {
    None
  }

  /**
   * The full cost of the purchase. This is the cost of the product plust
   * shipping and tax.
   *
   * @param basePrice the price of the product
   * @return the full price.
   */
  def total(basePrice: Money): Money = {
    val zero = Money.zero(CurrencyUnit.USD)

    basePrice
      .plus(shippingPrice.getOrElse(zero))
      .plus(tax.getOrElse(zero))
  }

  /**
   * Gets the selected PrintingOption or none if it hasn't yet been supplied.
   */
  def highQualityPrint: Option[PrintingOption] = {
    for (
      printingOptionString <- storefrontSession.get[String](Key.HighQualityPrint);
      printingOption <- PrintingOption(printingOptionString)
    ) yield {
      printingOption
    }

  }

  /** Returns a copy of this with a new PrintingOption */
  def withHighQualityPrint(printingOption: PrintingOption) = {
    this.withSession(storefrontSession.setting(Key.HighQualityPrint -> printingOption.name))
  }

  /**
   * Reads the shipping form out of the server session, or (optionally, preferentially)
   * out of the flash scope.
   *
   * @param flashOption flash out of which to preferentially read the form.
   */
  def shippingForm(flashOption: Option[Flash]=None): Option[CheckoutShippingForm] ={
    getFormFromFlashOrSession(formReaders.forShippingForm, flashOption)
  }

  /**
   * Reads the billing form out of the server session, or (optionally, preferentially) out
   * of the flash scope.
   *
   * @param flashOption flash out of which to preferentially read the form.
   */
  def billingForm(flashOption: Option[Flash]=None): Option[CheckoutBillingForm] = {
    // First read out the shipping form to tell if shipping info was the same as billing.
    val shippingFormOption = shippingForm(flashOption)
    val billingSameAsShippingOption = for(
      shipping <- shippingFormOption;
      billingSameAsShipping <- shipping.billingIsSameAsShipping.value
    ) yield {
      billingSameAsShipping
    }

    // We can only get an Option[ReadsForm[CheckoutBillingForm]] because, in certain cases, validating
    // the billing info is only possible in the presence of a shipping info. For example, if a high
    // quality print was specified and the user wants us to use the same billing as his shipping
    val maybeBillingReader = (this.highQualityPrint, billingSameAsShippingOption) match {
      case (Some(PrintingOption.HighQualityPrint), Some(true)) =>
        // If high quality print was specified, and billing is same as shipping, return the billing form
        // with the shipping form dependency provided
        for (shipping <- shippingForm(flashOption)) yield {
          formReaders.forBillingForm(Some(shipping))
        }

      case (None, _) | (Some(PrintingOption.HighQualityPrint), None) =>
        // If either (a) the high printing option was unavailable or (b) it was
        // HighQualityPrint but the billing-same-as-shipping value was unavailable
        // then we have insufficient info to create the billing form.
        None

      case _ =>
        // If no high quality print was specified, or one was specified but the shipping
        // wasn't the same as the billing, instantiate without providing the shipping form
        // dependency to the billing form to aid in validation.
        Some(formReaders.forBillingForm(None))
    }

    // If the reader exists, use it to read the form from either the flash or the session.
    for (
      billingReader <- maybeBillingReader;
      form <- getFormFromFlashOrSession(billingReader, flashOption)
    ) yield {
      form
    }
  }

  /**
   * Reads the personalize form out of the server session, or (optionally, preferentially)
   * out of the flash scope.
   *
   * @param flashOption flash out of which to preferentially read the form.
   */
  def personalizeForm(flashOption: Option[Flash]=None): Option[PersonalizeForm] = {
    getFormFromFlashOrSession(formReaders.forPersonalizeForm, flashOption)
  }

  /**
   * Returns a copy of this object equipped with the provided form.
   * Call `save` to persist changes.
   */
  def withForm(form: Form[_]): PurchaseForms = {
    val writtenSession = form.write(storefrontSession.asFormWriteable).written
    this.withSession(writtenSession)
  }

  /**
   * Persists any differences between this object and the one that instantiated
   * it. For example, persists any changes due to `withPersonalizeForm`, `withProductId`,
   * etc.
   *
   * @return a copy of this object, saved.
   */
  def save(): PurchaseForms = {
    new PurchaseForms(formReaders, storefrontSession.save())
  }

  //
  // Redirections.
  // TODO: Refactor these into another class; they all do pretty similar things
  //
  /**
   * Returns the product's next active inventory batch on the right if there is one,
   * or redirects to a "Product lacks inventory" page on the left.
   * @param celebrityUrlSlug identifies the celebrity, for the redirect.
   * @param product the product to check for available inventory.
   */
  def nextInventoryBatchOrRedirect(celebrityUrlSlug: String, product: models.Product)
  : Either[Redirect, InventoryBatch] =
  {
    product.nextInventoryBatchToEnd.toRight(
      left=this.redirectToInsufficientInventoryPage(
        celebrityUrlSlug,
        product.urlSlug
      )
    )
  }


  /**
   * Returns the printing option on the right if one has been provided in a previously
   * supplied form. Otherwise returns a redirect to the page that provides the high
   * quality print on the left.
   *
   * @param celebrityUrlSlug identifies the celebrity for the redirect
   * @param productUrlSlug identifies the product for the redirect
   */
  def printingOptionOrRedirectToReviewForm(celebrityUrlSlug: String, productUrlSlug: String): Either[Redirect, PrintingOption] = {
    highQualityPrint.toRight(left= {
      val redirectAction = reverse(getStorefrontReview(celebrityUrlSlug, productUrlSlug)).url

      new Redirect(redirectAction)
    })
  }

  /**
   * Returns an Option[shipping form] on the right either if it was present or if it was absent but
   * its absence was expected (because no print has been ordered)
   *
   * @param celebrityUrlSlug identifies the celebrity for the redirect
   * @param productUrlSlug identifies the product for the redirect
   */
  def validShippingFormOptionOrRedirectToCheckout(celebrityUrlSlug: String, productUrlSlug: String)
  : Either[Redirect, Option[(CheckoutShippingForm, CheckoutShippingForm.Valid)]] = {
    for (
      printingOption <- this.printingOptionOrRedirectToReviewForm(celebrityUrlSlug, productUrlSlug).right;
      maybeShippingForms <- validShippingFormGivenPrintingOptionOrRedirect(
                              printingOption,
                              celebrityUrlSlug,
                              productUrlSlug
                            ).right
    ) yield {
      maybeShippingForms
    }
  }

  /**
   * Returns a valid checkout form on the right if it was present, or a redirect to the checkout
   * form on the right if it was absent.
   *
   * @param celebrityUrlSlug identifies the celebrity for the redirect
   * @param productUrlSlug identifies the product for the redirect
   */
  def validCheckoutFormsOrRedirectToCheckout(celebrityUrlSlug: String, productUrlSlug: String)
  : Either[Redirect, (CheckoutBillingForm.Valid, Option[CheckoutShippingForm.Valid])] = {
    lazy val redirectToCheckout = this.redirectToCheckout(celebrityUrlSlug, productUrlSlug)

    for (
      billingForm <- billingForm().toRight(left=redirectToCheckout).right;

      validBillingForm <- billingForm.errorsOrValidatedForm.left.map(errors => redirectToCheckout).right;

      maybeShippingForms <- validShippingFormOptionOrRedirectToCheckout(celebrityUrlSlug, productUrlSlug).right
    ) yield {
      (validBillingForm, maybeShippingForms.map(forms => forms._2))
    }
  }

  /**
   * Returns the product ID being purchased on the right if one has been provided in a previously
   * supplied form. Otherwise returns a redirect to the page that allows the user to choose
   * a product on the left.
   *
   * @param celebrity the celebrity for the redirect
   * @param product the product for the redirect
   */
  def matchProductIdOrRedirectToChoosePhoto(celebrity:Celebrity, product:Product): Either[Redirect, Long] = {
    lazy val thisChoosePhotoRedirect = choosePhotoRedirect(celebrity.urlSlug.getOrElse("/"))

    // Redirect if either this form has no productId or the provided product Id didn't match
    for (
      productId <- productId.toRight(left=thisChoosePhotoRedirect).right;
      _ <- (if (productId == product.id) Right() else Left(thisChoosePhotoRedirect)).right
    ) yield {
      productId
    }
  }

  /**
   * Returns a validated PersonalizeForm on the right if one has been provided in a previously
   * supplied form. Otherwise returns a redirect to the page that allows the user to
   * personalize his egraph purchase on the left.
   *
   * @param celebrityUrlSlug identifies the celebrity for the redirect
   * @param productUrlSlug identifies the product for the redirect
   */
  def validPersonalizeFormOrRedirectToPersonalizeForm(celebrityUrlSlug: String, productUrlSlug: String)
  : Either[Redirect, PersonalizeForm.Validated] = {
    for (
      personalizeForm <- personalizeFormOrRedirectToPersonalizeForm(
        celebrityUrlSlug,
        productUrlSlug
      ).right;
      valid <-  personalizeForm.errorsOrValidatedForm.left.map { formError =>
        val action = reverse(getStorefrontPersonalize(
          celebrityUrlSlug,
          productUrlSlug
        ))

        new Redirect(action.url)
      }.right
    ) yield {
      valid
    }
  }


  //
  // Private members
  //
  private def withSession(storefrontSession: ServerSession): PurchaseForms = {
    new PurchaseForms(formReaders, storefrontSession)
  }

  private def getFormFromFlashOrSession[FormType <: Form[_]](
    reader: ReadsForm[FormType],
    flashOption: Option[Flash]
    ): Option[FormType] =
  {
    val maybeFormFromFlash = for (
      flash <- flashOption;
      form <- reader.read(flash.asFormReadable)
    ) yield {
      form
    }

    maybeFormFromFlash.orElse(reader.read(storefrontSession.asFormReadable))
  }

  private def redirectToCheckout(celebrityUrlSlug: String, productUrlSlug: String): Redirect = {
    new Redirect(reverse(getStorefrontCheckout(celebrityUrlSlug, productUrlSlug)).url)
  }

  private def validShippingFormGivenPrintingOptionOrRedirect(
    printingOption: PrintingOption,
    celebrityUrlSlug: String,
    productUrlSlug: String
    ): Either[Redirect, Option[(CheckoutShippingForm, CheckoutShippingForm.Valid)]] = {
    val maybeShippingForm = shippingForm()

    printingOption match {
      case PrintingOption.HighQualityPrint =>
        for (
          shippingForm <- maybeShippingForm.toRight(left=redirectToCheckout(celebrityUrlSlug, productUrlSlug)).right;
          validShippingForm <- shippingForm.errorsOrValidatedForm.left.map(
            errors => redirectToCheckout(celebrityUrlSlug, productUrlSlug)
          ).right
        ) yield {
          Some((shippingForm, validShippingForm))
        }

      case PrintingOption.DoNotPrint =>
        Right(None)
    }
  }

  private def redirectToInsufficientInventoryPage(celebrityUrlSlug: String, productUrlSlug: String) = {
    // TODO: Make this actually redirect to a page that shows insufficient inventory
    new Redirect(reverse(getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url)
  }

  private def choosePhotoRedirect(celebrityUrlSlug: String): Redirect = {
    new Redirect(reverse(getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url)
  }

  private def personalizeFormOrRedirectToPersonalizeForm(celebrityUrlSlug: String, productUrlSlug: String)
  : Either[Redirect, PersonalizeForm] = {

    personalizeForm().toRight(left= {
      val action = reverse(getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug))
      new Redirect(action.url)
    })
  }

}

object PurchaseForms {
  object Key {
    val ProductId = "productId"
    val HighQualityPrint = "order.review.highQualityPrint"
  }

  /**
   * Returns the most appropriate displayable string for a given combination of WrittenMessageRequest
   * and Option[String].
   *
   * @param messageRequest the option specified by the user in the order. For example `SignatureOnly`
   * @param messageText the text specified by the user in the order, if appropriate given the `messageRequest`.
   *
   * @return a user-presentable string.
   */
  def makeTextForCelebToWrite(messageRequest: WrittenMessageRequest, messageText: Option[String])
  : String = {
    messageRequest match {
      // TODO: Make these strings respond to gender
      case WrittenMessageRequest.SignatureOnly => "His signature only."
      case WrittenMessageRequest.CelebrityChoosesMessage => "Whatever he wants."
      case WrittenMessageRequest.SpecificMessage => messageText.getOrElse("")
    }
  }

  case class AllShippingForms(
    productId: Long,
    inventoryBatch: InventoryBatch,
    personalization: PersonalizeForm.Validated,
    billing: CheckoutBillingForm.Valid,
    shipping: Option[CheckoutShippingForm.Valid]
  )
}

class PurchaseFormFactory @Inject()(
  formReaders: PurchaseFormReaders,
  serverSessions: ServerSessionFactory
) {
  def formsForStorefront(celebrityId: Long) = {
    new PurchaseForms(
      formReaders,
      serverSessions.celebrityStorefrontCart(celebrityId)
    )
  }
}