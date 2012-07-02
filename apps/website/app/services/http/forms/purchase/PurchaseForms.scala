package services.http.forms.purchase

import services.http.{ServerSessionFactory, ServerSession}
import com.google.inject.Inject
import play.mvc.results.Redirect
import controllers.WebsiteControllers
import models.{InventoryBatch, Celebrity, Product}
import controllers.WebsiteControllers.{getStorefrontPersonalize, getStorefrontReview, reverse, getStorefrontChoosePhotoTiled}
import org.joda.money.{CurrencyUnit, Money}
import models.enums.PrintingOption

class PurchaseForms @Inject()(
  formReaders: PurchaseFormReaders,
  storefrontSession: ServerSession
) {
  import services.http.forms.Form.Conversions._

  import PurchaseForms.Key

  def productId: Option[Long] = {
    storefrontSession[Long](Key.ProductId)
  }

  def shippingPrice: Option[Money] = {
    None
  }

  def tax: Option[Money] = {
    None
  }

  def total(basePrice: Money): Money = {
    val zero = Money.zero(CurrencyUnit.USD)

    basePrice
      .plus(shippingPrice.getOrElse(zero))
      .plus(tax.getOrElse(zero))
  }

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

  def withProductId(productId: Long): PurchaseForms = {
    this.withSession(storefrontSession.setting(Key.ProductId -> productId))
  }

  def highQualityPrint: Option[PrintingOption] = {
    storefrontSession.get(Key.HighQualityPrint)
  }

  def withHighQualityPrint(doPrint: PrintingOption) = {
    this.withSession(storefrontSession.setting(Key.HighQualityPrint -> doPrint))
  }

  def highQualityPrintOrRedirectToReviewForm(celebrityUrlSlug: String, productUrlSlug: String): Either[Redirect, PrintingOption] = {
    highQualityPrint.toRight(left= {
      val redirectAction = reverse(getStorefrontReview(celebrityUrlSlug, productUrlSlug)).url

      new Redirect(redirectAction)
    })
  }

  def personalizeForm(flashOption: Option[play.mvc.Scope.Flash]=None): Option[PersonalizeForm] = {
    val formReader = formReaders.forPersonalizeForm

    flashOption.map(flash => formReader.read(flash.asFormReadable)).getOrElse {
      formReader.read(storefrontSession.asFormReadable)
    }
  }

  def personalizeFormOrRedirectToPersonalizeForm(celebrityUrlSlug: String, productUrlSlug: String)
  : Either[Redirect, PersonalizeForm] = {

    personalizeForm().toRight(left= {
      val action = reverse(getStorefrontPersonalize(celebrityUrlSlug, productUrlSlug))
      new Redirect(action.url)
    })
  }

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

  def withPersonalizeForm(form: PersonalizeForm) = {
    this.withSession(form.write(storefrontSession.asFormWriteable).written)
  }

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

  def redirectToInsufficientInventoryPage(celebrityUrlSlug: String, productUrlSlug: String) = {
    // TODO: Make this actually redirect to a page that shows insufficient inventory
    new Redirect(reverse(getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url)
  }

  def choosePhotoRedirect(celebrityUrlSlug: String): Redirect = {
    new Redirect(reverse(getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url)
  }

  def save(): PurchaseForms = {
    new PurchaseForms(formReaders, storefrontSession.save())
  }

  //
  // Private members
  //
  private def withSession(storefrontSession: ServerSession): PurchaseForms = {
    new PurchaseForms(formReaders, storefrontSession)
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