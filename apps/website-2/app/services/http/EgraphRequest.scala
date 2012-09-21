package services.http

import models.Egraph
import play.api.mvc.WrappedRequest
import play.api.mvc.Request

// TODO: PLAY20 migration. Test and comment this summbitch
case class EgraphRequest[A](
  egraph: Egraph,
  private val request: Request[A]
) extends WrappedRequest(request)
