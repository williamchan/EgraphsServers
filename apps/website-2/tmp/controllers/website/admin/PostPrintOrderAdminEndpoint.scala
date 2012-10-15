package controllers.website.admin

import models._
import play.api.mvc.Results.Redirect
import play.api.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}
import services.print.PrintManufacturingInfo

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
          printOrder.copy(pngUrl = printOrder.getPngUrl).save()
          new Redirect(GetPrintOrderAdminEndpoint.url(printOrderId).url)
        }
        case "generateFramedPrintImage" => {
          val framedPrintImageData = printOrder.getFramedPrintImageData.getOrElse(("", ""))
          <html>
            <body>
              <a href={framedPrintImageData._1} target="_blank">{framedPrintImageData._1}</a>
              <br/>
              {PrintManufacturingInfo.headerCSVLine}
              <br/>
              {framedPrintImageData._2}
            </body>
          </html>

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