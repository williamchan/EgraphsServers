package controllers.website.admin

import models._
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import services.http.SafePlayParams.Conversions._
import services.logging.Logging
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import services.print.PrintManufacturingInfo

trait PostPrintOrderAdminEndpoint { this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def printOrderStore: PrintOrderStore

  def postPrintOrderAdmin(printOrderId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        printOrderStore.findById(printOrderId) match {
          case None => NotFound("No print order with that id")
          case Some(printOrder) => {
            val action = Form("action" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
            action match {
              case "markFulfilled" =>
	          	printOrder.copy(isFulfilled = true).save()
	          	Redirect(GetPrintOrderAdminEndpoint.url(printOrderId))
              case "generatePNG" => {
                printOrder.copy(pngUrl = printOrder.getPngUrl).save()
                Redirect(GetPrintOrderAdminEndpoint.url(printOrderId))
              }
              case "generateFramedPrintImage" => {
                val framedPrintImageData = printOrder.getFramedPrintImageData.getOrElse(("", ""))
                Ok(views.html.Application.admin.admin_printinfo(
                  framedPrintImageData._1,
                  PrintManufacturingInfo.headerCSVLine,
                  framedPrintImageData._2,
                  printOrder.getStandaloneCertificateUrl
                ))
              }
              case "editAddress" => {
                val shippingAddress = Form(single("shippingAddress" -> text)).bindFromRequest.apply("shippingAddress").value.get
                printOrder.copy(shippingAddress = shippingAddress).save()
                Redirect(GetPrintOrderAdminEndpoint.url(printOrderId))
              }
              case _ => Forbidden("Unsupported operation")
            }
          }
        }
      }
    }
  }
}