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

trait PurchaseFlowPersonalizationEndpoints extends ImplicitHeaderAndFooterData { this: Controller =>
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
    httpFilters.requireCelebrityUrlSlug(celebrityUrlSlug) { maybeUnpublishedCelebrity =>
      httpFilters.requireAdministratorLogin.inSessionOrUseOtherFilter(maybeUnpublishedCelebrity)(
        otherFilter = httpFilters.requireCelebrityPublishedAccess.filter((maybeUnpublishedCelebrity, accesskey))
      ) { celeb =>
        Action { implicit request =>
          val products = celeb.productsInActiveInventoryBatches()
          val maybeMostExpensive = products.sortBy(_.price.getAmount).headOption

          val productViews = for (
            product <- products;
            mostExpensive <- maybeMostExpensive.toSeq
          ) yield {
            product.asPersonalizeThumbView.copy(selected=product == mostExpensive)
          }
          val starView = celeb.asPersonalizeStar(productViews)

          Ok(views.html.frontend.storefronts.a.personalize(
            starView,
            WebsiteControllers.getCheckout(celebrityUrlSlug, accesskey).url,
            maxDesiredTextChars=EgraphForm.maxDesiredTextChars,
            maxMessageToCelebChars=EgraphForm.maxMessageToCelebChars
          ))
        }
      }
    }
  }

  def getCheckout(celebrityUrlSlug: String, accesskey: String = ""): Action[AnyContent] = controllerMethod.withForm() { implicit authenticityToken =>
    httpFilters.requireCelebrityUrlSlug(celebrityUrlSlug) { celeb =>
      Action { implicit request =>
        // Only serve the page if there's a valid Egraph order in the checkout
        checkouts.decacheOrCreate(celeb.id).order.map { form =>
          Ok(views.html.frontend.storefronts.a.checkout(
            celebId=celeb.id,
            personalizeUrl=WebsiteControllers.getPersonalize(celebrityUrlSlug, accesskey).url,
            paymentJsModule=payment.browserModule,
            paymentPublicKey=payment.publishableKey
          ))
        }.getOrElse {
          Redirect(controllers.routes.WebsiteControllers.getPersonalize(celebrityUrlSlug))
        }
      }
    }
  }
}
