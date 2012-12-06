package controllers

import play.api._
import play.api.mvc._

object MyToyBox extends ToyBox {
  // Set the path to be used for login page requests
  val loginPath = "/login"
  def assetsRoute = routes.Assets.at
}