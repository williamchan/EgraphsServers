package services.http

import models.Celebrity
import play.api.mvc.WrappedRequest
import play.api.mvc.Request

// TODO: PLAY20 migration. Test and comment this summbitch
case class CelebrityRequest[A](
  celeb: Celebrity,
  private val request: Request[A]
) extends WrappedRequest(request)
