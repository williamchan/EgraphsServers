package services.http

import models.Administrator
import play.api.mvc.WrappedRequest
import play.api.mvc.Request

// TODO: PLAY20 migration. Test and comment this summbitch
case class AdminRequest[A](
  admin: Administrator,
  private val request: Request[A]
) extends WrappedRequest(request)
