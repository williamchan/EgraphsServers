package models.checkout.forms

import play.api.data.Form
import play.api.mvc.Request


trait CheckoutForm[T] {
  def bindFromRequest()(implicit request: Request[_]) = form.bindFromRequest()(request)
  def form: Form[T]
}
