package controllers.website.consumer

import models.{CustomerStore, OrderStore}
import play.api.mvc.{Action, Controller}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import services.mvc.ImplicitHeaderAndFooterData
import services.pdf.EgraphExplanationPdf
import services.{TempFile, Utils}

private[controllers] trait GetEgraphExplanationCardEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def customerStore: CustomerStore
  protected def orderStore: OrderStore

  def getEgraphExplanationCard(orderId: Long) = controllerMethod() {
    httpFilters.requireCustomerLogin.inSession() { case (customer, account) =>
      httpFilters.requireOrderId(orderId) { order =>
        Action { implicit request =>

          if (order.isBuyerOrRecipient(Some(customer.id))) {
            val pdfByteStream = EgraphExplanationPdf.generate(recipientName = order.recipientName,
              buyerName = order.buyer.name,
              celebrityName = order.product.celebrity.publicName)
            val pdfFile = TempFile.named("Egraph-gift-" + orderId + ".pdf")
            Utils.saveToFile(pdfByteStream.toByteArray, pdfFile)
            Ok.sendFile(pdfFile, inline = true)

          }
          else {
            Forbidden("Doh! Were you looking for an egraph explanation card? Visit your Account Gallery to view your pending egraphs.")
          }
        }
      }
    }
  }
}
