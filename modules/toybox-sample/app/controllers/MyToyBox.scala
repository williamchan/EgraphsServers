package controllers

import play.api._
import play.api.mvc._

object MyToyBox extends ToyBox {
  // Must point ToyBox to the right routes
  def maybePostLoginRoute = Some(routes.MyToyBox.postLogin)
  def maybeGetLoginRoute = Some(routes.MyToyBox.getLogin) 
  def maybeAssetsRoute = Some(routes.Assets.at)
}