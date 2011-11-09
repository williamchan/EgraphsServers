package controllers

import play.mvc._
import com.stripe.model.Charge
import play.data.validation.Required
import sun.misc.BASE64Decoder
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

object EGraphs extends Controller {
  def post(orderId: String, @Required signature: String, @Required audio: String) = {
    if (validation.hasErrors) {
      "Errors: " + validation.errorsMap().toString + "\n"
    }
    else {
      "received signature = " + params.get("signature") + " and audio = " + params.get("audio") +
        " for orderId " + orderId + " by " + request.user + ":" + request.password + "\n"
    }
  }
}

object test extends Controller {

  def base64decode(str: String): String = {
    new String(Codec.decodeBASE64(str)) + "\n"
  }

  def json = {
    Json("[{'orderId': 1,'celebrityId': 1,'price': 100.00,'status': 'delivered'}]")
  }

  def script = {
    "Hello World"
  }

}