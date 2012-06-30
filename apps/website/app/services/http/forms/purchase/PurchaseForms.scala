package services.http.forms.purchase

import services.http.{ServerSessionFactory, ServerSession}
import com.google.inject.Inject

class PurchaseForms @Inject()(storefrontSession: ServerSession) {
  import PurchaseForms.Key

  def productId: Option[Long] = {
    storefrontSession[Long](Key.ProductId)
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