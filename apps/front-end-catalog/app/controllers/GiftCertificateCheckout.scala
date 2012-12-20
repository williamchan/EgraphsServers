package controllers

import play.api.mvc.{Action, Controller}
import helpers.DefaultImplicitTemplateParameters

object GiftCertificateCheckout extends Controller with DefaultImplicitTemplateParameters {
  def index = Action {
    Ok(views.html.frontend.gift_certificate_checkout())
  }
}
