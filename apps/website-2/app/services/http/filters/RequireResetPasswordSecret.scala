package services.http.filters

import com.google.inject.Inject

import models.Account
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.data.Form
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.Forbidden
import play.api.mvc.Action

class RequireResetPasswordSecret @Inject() () {

  def apply[A](account: Account)(action: Action[A]): Action[A] = {
    Action(action.parser) { implicit request =>
      Form(single("secretKey" -> text)).bindFromRequest.fold(
        errors => BadRequest("Reset password key was required but not provided"),
        resetPasswordKey => {
          if (account.verifyResetPasswordKey(resetPasswordKey)) {
            action(request)
          } else {
            Forbidden("The password reset URL you used is either out of date or invalid.")
          }
        })
    }
  }
}
