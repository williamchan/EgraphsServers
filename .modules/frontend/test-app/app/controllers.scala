package controllers

import play._
import play.mvc._

object Application extends Controller {
  import views.frontend
  
  def index = {
    frontend.html.test("Hello Derp")
//    html.index("Your Scala application is ready!")
  }
  
}
