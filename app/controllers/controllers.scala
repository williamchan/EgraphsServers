package controllers

import play.mvc._
import com.stripe.model.Charge

object Application extends Controller {

  import views.Application._

  def index = {
    html.index()
  }
}

object stripe extends Controller {
  def stripe = {
    views.html.stripe("")
  }

  def post(stripeToken: String) = {
    val chargeWithTokenParams: Map[String, AnyRef] = Map(
      "amount" -> new java.lang.Integer(1000),
      "currency" -> "usd",
      "card" -> stripeToken
    )
    val charge: Charge = Charge.create(scala.collection.JavaConversions.asJavaMap(chargeWithTokenParams))
  }
}
