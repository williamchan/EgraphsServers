package controllers

import play.api._
import play.api.mvc._

object MyToyBox extends ToyBox {
  // Must point ToyBox to the right routes or use None to default to preset routes
  def maybePostLoginRoute = None //Some(routes.MyToyBox.postLogin)
  def maybeGetLoginRoute = None //Some(routes.MyToyBox.getLogin) 
  def maybeAssetsRoute = Some(routes.Assets.at)
}