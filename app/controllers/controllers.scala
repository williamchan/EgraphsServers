package controllers

import models._
import play.mvc._
import play.db.jpa.JPABase
import util.Random

object Application extends Controller {

  import views.Application._

  def index = {
    html.index("Your Scala application is ready!")
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

  /* // For Anorm
  def celebrities = {
    val allCelebrities: Seq[Celebrity] = Celebrity.find().list()
    html.celebrities(allCelebrities)
  }

  def products = {
    val allProducts: Seq[Product] = Product.find().list()
    html.products(allProducts)
  }*/


}