package controllers

import play.api._
import play.api.mvc._

object MyToyBox extends ToyBox {
  def maybePostLoginRoute = Some(routes.MyToyBox.postLogin)
  def maybeGetLoginRoute  = Some(routes.MyToyBox.getLogin) 

  /*
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    println(request.method + ", " + request.path + ", " + request.queryString)
    
    val handler = super.onRouteRequest(request)
    handler match {
      case Some(action: Action[_]) =>
        println("Got action: " + action)
      case _ => 
    }

    handler
  }

  private def printCookies(request: RequestHeader, cookieNames: List[String]) {
    for (cookieName <- cookieNames) {
      request.cookies.get(cookieName) match {
        case Some(cookie: Cookie) =>
          println(cookieName + ": " + cookie)
        case None =>
      }
    }
  }
  */
}