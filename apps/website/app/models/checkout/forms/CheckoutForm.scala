package models.checkout.forms

import play.api.data.Form
import play.api.mvc.Request
import models.checkout.EgraphCheckoutAdapter
import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits._
import services.logging.Logging
import play.api.libs.json._

/** Convenience trait for wrapping Play forms */
trait CheckoutForm[T] {
  import CheckoutForm._

  def bindFromRequest()(implicit request: Request[_]) = form.bindFromRequest()(request)

  def bindFromRequestAndCache(checkout: EgraphCheckoutAdapter)(implicit request: Request[_])
  : Form[T] =
  {
    val form = bindFromRequest()
    log(s"Cacheing submitted resource ${this.getClass.getSimpleName}")
    log(s"  data: ${Json.toJson(form.data)}")
    log(s"  errors: ${form.errorsAsJson}")

    this.cache(checkout, Some(form))

    form
  }

  def form: Form[T]

  def cache(checkout: EgraphCheckoutAdapter, form: Option[Form[T]] = None)(implicit request: Request[_]) {
    val formToCache = form.getOrElse(this.bindFromRequest())

    checkout.cart.setting(this.getClass.getSimpleName -> formToCache.data).save()
  }

  def decache[FormT <: Form[T]](checkout: EgraphCheckoutAdapter)
    (implicit request: Request[_], mani: Manifest[FormT])
  : Option[Form[T]] =
  {
    checkout.cart.get[Map[String, String]](this.getClass.getSimpleName).map { formData =>
      form.bind(formData)
    }
  }
}

object CheckoutForm extends Logging