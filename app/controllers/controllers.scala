package controllers

import play.mvc._
import com.stripe.model.Charge
import play.libs.Codec

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

object test extends Controller {

  def base64decode(str: String): String = {
    new String(Codec.decodeBASE64(str)) + "\n"
  }

  def script = {
    "Hello world"
  }

}