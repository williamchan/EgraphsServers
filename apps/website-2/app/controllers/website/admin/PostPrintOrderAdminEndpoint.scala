package controllers.website.admin

import models._
import play.mvc.results.Redirect
import play.api.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}

trait PostPrintOrderAdminEndpoint { this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def printOrderStore: PrintOrderStore

  def postPrintOrderAdmin(printOrderId: Long) = postController() {
    adminFilters.requirePrintOrder {printOrder =>
      params.get("action") match {
        case "markFulfilled" =>
          printOrder.copy(isFulfilled = true).save()
          new Redirect(GetPrintOrderAdminEndpoint.url(printOrderId).url)
        case "generatePNG" => {
          val pngUrl = printOrder.generatePng()
          printOrder.copy(pngUrl = pngUrl).save()
          new Redirect(GetPrintOrderAdminEndpoint.url(printOrderId).url)
        }
        case "editAddress" => {
          val shippingAddress = params.get("shippingAddress")
          printOrder.copy(shippingAddress = shippingAddress).save()
          new Redirect(GetPrintOrderAdminEndpoint.url(printOrderId).url)
        }
        case _ => Forbidden("Unsupported operation")
      }
    }
  }
}