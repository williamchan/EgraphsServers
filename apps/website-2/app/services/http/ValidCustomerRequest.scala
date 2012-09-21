package services.http

import play.api.mvc.WrappedRequest
import models.Customer
import play.api.mvc.Request

// TODO: PLAY20 migration. Comment this.
case class ValidCustomerRequest[A] (
  customer: Customer,
  private val request: Request[A]
) extends WrappedRequest(request)
