package controllers.website.consumer

import play.api.mvc.{Action, AnyContent, Controller}
import services.http.filters.HttpFilters
import services.http.ControllerMethod
import services.mvc.ProductViewConversions._
import services.mvc.celebrity.CelebrityViewConversions._
import services.mvc.ImplicitHeaderAndFooterData
import models.checkout.forms.EgraphForm
import models.checkout.CheckoutAdapterServices
import services.payment.Payment
import controllers.routes.WebsiteControllers.getRootConsumerEndpoint

trait PurchaseFlowEndpoints extends ImplicitHeaderAndFooterData { this: Controller =>
  import controllers.routes.WebsiteControllers
  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def checkouts: CheckoutAdapterServices
  protected def payment: Payment

  //
  // Controllers
  //
  def getPersonalize(celebrityUrlSlug: String, accesskey: String = ""): Action[AnyContent] =
  controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireCelebrityUrlSlug(celebrityUrlSlug) { celebrity =>
      httpFilters.requireAdministratorLogin.inSessionOrUseOtherFilter(celebrity)(
        otherFilter = httpFilters.requireCelebrityPublishedAccess.filter((celebrity, accesskey))
      ) { accessibleCeleb =>
        Action { implicit request =>
          val products = accessibleCeleb.activeProductsAndInventoryBatches
            .filter { case (product, inventory) => inventory.hasInventory }
            .map { case (product, _) => product }

          val maybeCheapest = products.sortBy(_.price.getAmount).headOption

          val productViews = for (
            product <- products;
            cheapest <- maybeCheapest.toSeq
          ) yield {
            product.asPersonalizeThumbView.copy(selected=product == cheapest)
          }
          val starView = accessibleCeleb.asPersonalizeStar(productViews)

          Ok(views.html.frontend.storefronts.a.personalize(
            starView,
            WebsiteControllers.getCheckout(celebrityUrlSlug, accesskey).url,
            maxDesiredTextChars=EgraphForm.maxDesiredTextChars,
            maxMessageToCelebChars=EgraphForm.maxMessageToCelebChars
          ))

          redirectToHomepage
        }
      }
    }
  }

  def getCheckout(celebrityUrlSlug: String, accesskey: String = ""): Action[AnyContent] = controllerMethod.withForm() { implicit authenticityToken =>
    httpFilters.requireCelebrityUrlSlug(celebrityUrlSlug) { celeb =>
      Action { implicit request =>
        // Only serve the page if there's a valid Egraph order in the checkout
        checkouts.decacheOrCreate(celeb.id).order.map { _ =>
          Ok(views.html.frontend.storefronts.a.checkout(
            celebId=celeb.id,
            personalizeUrl=WebsiteControllers.getPersonalize(celebrityUrlSlug, accesskey).url,
            paymentJsModule=payment.browserModule,
            paymentPublicKey=payment.publishableKey
          ))

          redirectToHomepage
        }.getOrElse {
          Redirect(controllers.routes.WebsiteControllers.getPersonalize(celebrityUrlSlug))
        }
      }
    }
  }

  val redirectToHomepage = Redirect(getRootConsumerEndpoint())
}
