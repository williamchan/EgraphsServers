package controllers.api

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import models._
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import services.blobs.AccessPolicy

// TODO(egraph-exploration): Work in progress. Not finalized. Used for rapid prototyping.
private[controllers] trait GetCustomerEgraphsApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def orderStore: OrderStore
  protected def httpFilters: HttpFilters

  def getCustomerEgraphs = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCustomerId.inAccount(account) { customer =>
        Action {
          import FulfilledOrderBundle.FulfilledOrderBundleFormat._
          val egraphBundles = orderStore.findFulfilledForCustomer(customer)
          Ok(Json.arr(egraphBundles))
        }
      }
    }
  }
}

/* This is here because it is currently only used by the consumer mobile app prototype. */
case class FulfilledOrderBundle(egraph: Egraph, order: Order, product: Product, celebrity: Celebrity) {
  private val desiredWidth = 480

  private def imageUrl = egraph.getEgraphImage(LandscapeEgraphFrame.imageWidthPixels).getSavedUrl(accessPolicy = AccessPolicy.Public)
}

object FulfilledOrderBundle {
  implicit object FulfilledOrderBundleFormat extends Format[FulfilledOrderBundle] {
    def writes(bundle: FulfilledOrderBundle): JsValue = {
      import _root_.frontend.formatting.DateFormatting.Conversions._
      val FulfilledOrderBundle(egraph, order, product, celebrity) = bundle
      val messageToCelebrity = order.messageToCelebrity.getOrElse("")
      Json.obj(
        "orderId" -> order.id,
        "egraphId" -> egraph.id,
        "url" -> order.services.consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraphClassic(order.id).url),
        "image" -> bundle.imageUrl,
        "audio" -> egraph.assets.audioMp3Url,
        "video" -> "",
        "icon" -> product.iconUrl,
        "signedAt" -> egraph.getSignedAt.formatDayAsPlainLanguage,
        "messageToCelebrity" -> messageToCelebrity,
        "celebrityName" -> celebrity.publicName,
        "celebritySubtitle" -> celebrity.roleDescription,
        "celebrityMasthead" -> celebrity.landingPageImage.resizedWidth(bundle.desiredWidth).getSaved(AccessPolicy.Public, Some(0.8f)).url,
        "productTitle" -> product.name,
        "recipientName" -> order.recipientName,
        "recipientId" -> order.recipientId)
    }
  }
}
