package controllers.api.checkout

import play.api.mvc.{Result, AnyContent, Action, Controller}
import services.http.{ControllerMethod, POSTControllerMethod}
import services.http.filters.HttpFilters
import models.checkout.{EgraphCheckoutAdapter, CheckoutAdapterServices}
import models.checkout.forms.CheckoutForm
import play.api.libs.json._
import play.api.data.Form

trait CheckoutResourceEndpoint[T] { this: Controller =>
  protected def postController: POSTControllerMethod
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def checkoutAdapters: CheckoutAdapterServices
  protected def resourceForm: CheckoutForm[T]

  protected def setResource(resource: Option[T], checkout: EgraphCheckoutAdapter): EgraphCheckoutAdapter

  protected def postCheckoutResource(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = postController() {
    httpFilters.requireSessionAndCelebrityUrlSlugs(sessionIdSlug, checkoutIdSlug) { (sessionId, celeb) =>
      Action { implicit request =>
        val checkout = checkoutAdapters.decacheOrCreate(celeb.id)

        val (maybeResource, result): (Option[T], Result) = resourceForm.bindFromRequestAndCache(checkout).fold(
          formWithErrors => (None, BadRequest(formWithErrors.errorsAsJson)),
          resource => (Some(resource), Ok)
        )

        setResource(maybeResource, checkout).cache()

        result
      }
    }
  }

  protected def getCheckoutResource[FormT <: Form[T]](
    sessionIdSlug: UrlSlug,
    checkoutIdSlug: UrlSlug
  )(implicit mani: Manifest[FormT]): Action[AnyContent] = controllerMethod()
  {
    httpFilters.requireSessionAndCelebrityUrlSlugs(sessionIdSlug, checkoutIdSlug) { (sessionId, celeb) =>
      Action { implicit request =>
        val checkout = checkoutAdapters.decacheOrCreate(celeb.id)
        resourceForm.decache[FormT](checkout).map { cachedForm =>
          Ok(Json.toJson(cachedForm.data))
        }.getOrElse {
          NotFound
        }
      }
    }
  }
}