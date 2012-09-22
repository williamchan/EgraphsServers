package services.http

import models.Order
import play.api.mvc.WrappedRequest
import play.api.mvc.Request

// TODO: PLAY20 migration. Comment this
case class OrderRequest[A](
  order: Order,
  private val request: Request[A]
) extends WrappedRequest(request)
