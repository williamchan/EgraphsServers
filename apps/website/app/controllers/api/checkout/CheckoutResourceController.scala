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
import services.logging.Logging

class CheckoutResourceControllerFactory @Inject() (services: CheckoutResourceControllerServices) {
  /**
   * Creates controller instances for checkout subresources (e.g. payment, egraph, recipient, buyer)
   *
   * @param resourceForm the [[models.checkout.forms.CheckoutForm]] for the resource of interest,
   *                     e.g. [[models.checkout.forms.CouponForm]]
   * @param setResource a setter function that provides an optional instance of the
   *                    parameterized type T to an [[models.checkout.EgraphCheckoutAdapter]]
   * @tparam T the domain object that this checkout form produces. Usually a type of
   *           [[models.checkout.LineItemType]]
   * @return an object with post and get functions, each of which accepts a sessionIdSlug: String and
   *         a checkoutIdSlug: Long (which should be a celebrityId)
   */
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
  import CheckoutResourceController._

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
          val formJson = Json.toJson(cachedForm.data)
          log(s"OK: Found previously submitted form for ${resourceForm.getClass.getSimpleName}")
          log(Json.stringify(formJson))
          Ok(formJson)
        }.getOrElse {
          log(s"NOT_FOUND: No cached form found for ${resourceForm.getClass.getSimpleName}")
          NotFound
        }
      }
    }
  }
}

object CheckoutResourceController extends Logging