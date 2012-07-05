package services.http.forms.purchase

import services.http.{ServerSessionFactory, ServerSession}
import com.google.inject.Inject
import play.mvc.results.Redirect
import models.{InventoryBatch, Celebrity, Product}
import controllers.WebsiteControllers.{getStorefrontPersonalize, getStorefrontReview, reverse, getStorefrontChoosePhotoTiled}
import org.joda.money.{CurrencyUnit, Money}
import models.enums.PrintingOption
import services.http.forms.{Form, ReadsForm}
import play.mvc.Scope.Flash

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

      case (None, _) | (_, None) =>
        // If either the high quality print status or billing-same-as-shipping status were
        // unavailable then we have insufficient info to create the billing form.
        None

      case _ =>
        // If no high quality print was specified, or one was specified but the shipping
        // wasn't the same as the billing, do not provide the shipping form dependency
        // to the billing form for validation.
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
  def personalizeForm(flashOption: Option[play.mvc.Scope.Flash]=None): Option[PersonalizeForm] = {
    val formReader = formReaders.forPersonalizeForm

    flashOption.map(flash => formReader.read(flash.asFormReadable)).getOrElse {
      formReader.read(storefrontSession.asFormReadable)
    }
  }

  /**
   * Returns a copy of this object equipped with the provided personalize form.
   * Call `save` to persist changes.
   */
  def withPersonalizeForm(form: PersonalizeForm) = {
    this.withSession(form.write(storefrontSession.asFormWriteable).written)
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
  def highQualityPrintOrRedirectToReviewForm(celebrityUrlSlug: String, productUrlSlug: String): Either[Redirect, PrintingOption] = {
    highQualityPrint.toRight(left= {
      val redirectAction = reverse(getStorefrontReview(celebrityUrlSlug, productUrlSlug)).url

      new Redirect(redirectAction)
    })
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
    flashOption.map(flash => reader.read(flash.asFormReadable)).getOrElse {
      reader.read(storefrontSession.asFormReadable)
    }
  }

  private def redirectToInsufficientInventoryPage(celebrityUrlSlug: String, productUrlSlug: String) = {
    // TODO: Make this actually redirect to a page that shows insufficient inventory
    new Redirect(reverse(getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url)
  }

  private def choosePhotoRedirect(celebrityUrlSlug: String): Redirect = {
    new Redirect(reverse(getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url)
  }

  def personalizeFormOrRedirectToPersonalizeForm(celebrityUrlSlug: String, productUrlSlug: String)
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