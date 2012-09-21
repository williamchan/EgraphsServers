package services.http

import models.PrintOrder
import play.api.mvc.WrappedRequest
import play.api.mvc.Request

// TODO: PLAY20 migration. Test and comment this summbitch
case class PrintOrderRequest[A](
  printOrder: PrintOrder,
  private val request: Request[A]
) extends WrappedRequest(request)
