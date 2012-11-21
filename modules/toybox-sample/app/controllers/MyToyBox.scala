package controllers

import play.api._
import play.api.mvc._

object MyToyBox extends ToyBox {
  // Must point ToyBox to the right routes
  def maybePostLoginRoute = Some(routes.MyToyBox.postLogin)
  def maybeGetLoginRoute  = Some(routes.MyToyBox.getLogin) 


  // This is unnecessary, just for testing
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {    
    val maybeHandler = super.onRouteRequest(request)

    // Inspect request and its (option of) handler here

    maybeHandler
  }


  // helper for inspecting cookies
  /*private def printCookies(request: RequestHeader, cookieNames: List[String]) {
    for (cookieName <- cookieNames) {
      request.cookies.get(cookieName) match {
        case Some(cookie: Cookie) =>
          println(cookieName + ": " + cookie)
        case None =>
      }
    }
  }*/
}