package controllers

import play.api._
import play.api.mvc._

object MyToyBox extends ToyBox {
  // Must point ToyBox to the right routes or use None to default to preset routes
  val loginPath = "/login"
  def assetsRoute = routes.Assets.at
}