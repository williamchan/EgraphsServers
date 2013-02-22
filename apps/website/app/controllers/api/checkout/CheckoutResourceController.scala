package controllers.api.checkout

import play.api.mvc._
import services.http.{POSTApiControllerMethod, ControllerMethod, POSTControllerMethod}
import services.http.filters.HttpFilters
import models.checkout.{EgraphCheckoutAdapter, CheckoutAdapterServices}
import models.checkout.forms.CheckoutForm
import play.api.libs.json._
import play.api.data.Form
import models.checkout.CheckoutAdapterServices
import models.checkout.EgraphCheckoutAdapter
import scala.Some
import com.google.inject.Inject

class CheckoutResourceControllerFactory @Inject() (services: CheckoutResourceControllerServices) {
  def apply[T](
    resourceForm: CheckoutForm[T],
    setResource: (Option[T], EgraphCheckoutAdapter) => EgraphCheckoutAdapter
  ): CheckoutResourceController[T] = {
    new CheckoutResourceController[T](resourceForm, setResource, services)
  }
}

case class CheckoutResourceControllerServices @Inject() (
  postApiController: POSTApiControllerMethod,
  controllerMethod: ControllerMethod,
  httpFilters: HttpFilters,
  checkoutAdapters: CheckoutAdapterServices
)

class CheckoutResourceController[T] (
  resourceForm: CheckoutForm[T],
  setResource: (Option[T], EgraphCheckoutAdapter) => EgraphCheckoutAdapter,
  services: CheckoutResourceControllerServices
) extends Results
{
  import services._

  def post(sessionIdSlug: UrlSlug, checkoutIdSlug: Long): Action[AnyContent] = {
    postApiController() {
      httpFilters.requireSessionAndCelebrityUrlSlugs(sessionIdSlug, checkoutIdSlug) { (sessionId, celeb) =>
        Action { implicit request =>
          val checkout = checkoutAdapters.decacheOrCreate(celeb.id)

          val (maybeResource, result): (Option[T], Result) = resourceForm.bindFromRequestAndCache(checkout).fold(
            formWithErrors => (None, BadRequest(Json.obj("errors" -> formWithErrors.errorsAsJson))),
            resource => (Some(resource), Ok)
          )

          setResource(maybeResource, checkout).cache()

          result
        }
      }
    }
  }

  def get[FormT <: Form[T]](
    sessionIdSlug: UrlSlug,
    checkoutIdSlug: Long
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