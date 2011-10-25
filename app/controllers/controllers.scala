package controllers

import models._
import play.mvc._
import play.db.jpa.JPABase
import com.stripe.model.Charge
import com.stripe.Stripe

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
      "amount" -> new Integer(1000),
      "currency" -> "usd",
      "card" -> stripeToken
    )
    val charge: Charge = Charge.create(scala.collection.JavaConversions.asJavaMap(chargeWithTokenParams))
  }
}

object test extends Controller {

  def json = {
    Json("[{'orderId': 1,'celebrityId': 1,'price': 100.00,'status': 'delivered'}]")
  }

  def script = {
    "Hello World"

    var kevin = JPABase.em().find(classOf[Celebrity], 1L)
    if (kevin == null) {
      "kevin not found"
    } else {
      "kevin found"
    }
  }

}