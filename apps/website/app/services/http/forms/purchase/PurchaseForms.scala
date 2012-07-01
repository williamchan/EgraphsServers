package services.http.forms.purchase

import services.http.{ServerSessionFactory, ServerSession}
import com.google.inject.Inject
import play.mvc.results.Redirect
import controllers.WebsiteControllers
import models.{Celebrity, Product}


class PurchaseForms @Inject()(
  storefrontSession: ServerSession
) {
  import PurchaseForms.Key

  def productId: Option[Long] = {
    storefrontSession[Long](Key.ProductId)
  }

  def productIdOrChoosePhotoRedirect(celebrity:Celebrity, product:Product): Either[Redirect, Long] = {
    lazy val thisChoosePhotoRedirect = choosePhotoRedirect(celebrity.urlSlug.getOrElse("/"))

    // Redirect if either this form has no productId or the provided product Id didn't match
    for (
      productId <- productId.toRight(left=thisChoosePhotoRedirect).right;
      _ <- (if (productId == product.id) Right() else Left(thisChoosePhotoRedirect)).right
    ) yield {
      productId
    }
  }

  def choosePhotoRedirect(celebrityUrlSlug: String): Redirect = {
    import WebsiteControllers.{reverse, getStorefrontChoosePhotoTiled}

    new Redirect(reverse(getStorefrontChoosePhotoTiled(celebrityUrlSlug)).url)
  }

  def withProductId(productId: Long): PurchaseForms = {
    this.withSession(storefrontSession.setting(Key.ProductId -> productId))
  }

  def save(): PurchaseForms = {
    new PurchaseForms(storefrontSession.save())
  }

  //
  // Private members
  //
  private def withSession(storefrontSession: ServerSession): PurchaseForms = {
    new PurchaseForms(storefrontSession)
  }
}

object PurchaseForms {
  object Key {
    val ProductId = "productId"
  }
}

class PurchaseFormFactory @Inject()(serverSessions: ServerSessionFactory) {
  def formsForStorefront(celebrityId: Long) = {
    new PurchaseForms(serverSessions.celebrityStorefrontCart(celebrityId))
  }
}