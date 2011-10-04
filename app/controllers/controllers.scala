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
    Celebrity.getClass.getSimpleName

    var kevin = JPABase.em().find(Celebrity.me.getClass, 1L).asInstanceOf[Celebrity]
    if (kevin == null) {
      "kevin not found"
    } else {
      "kevin found"
    }
  }

  def posts = {
    var post: Post_ScalaJPA = new Post_ScalaJPA()
    post.title = "Post_ScalaJPA " + Random.nextInt()
    post.body = "herp derp"
    post.save()
    //    post.title

    post = JPABase.em().find(post.getClass(), 1L).asInstanceOf[Post_ScalaJPA]

    post.body = "derpa derp"
    post.save()

    post = JPABase.em().find(post.getClass(), 1L).asInstanceOf[Post_ScalaJPA]
    post.body

  }

  def users = {
    var user: User_JavaJPA = new User_JavaJPA()
    user.name = "William" + Random.nextInt()
    user.email = user.name + "@gmail.com"
    user.address = "Here"
    user.save_Scala()
    //    user.create_Scala()
    //    user.validateAndSave()
    //    user.validateAndCreate_Scala()
    //    user = user.refresh_Scala()
    //    val me: List[JPABase] = user.me()
    //    me.size();
    //    "" + user.email + " created"

    //    user.address = "There"
    //    user.save_Scala()
    //    user.address

    //    user.delete_Scala()

    //    "Hello"
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