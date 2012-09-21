package services.http

import models.Account
import play.api.mvc.WrappedRequest
import play.api.mvc.Request

// TODO: PLAY20 migration. Test and comment this summbitch
case class AccountRequest[A](
  account: Account,
  private val request: Request[A]
) extends WrappedRequest(request)
