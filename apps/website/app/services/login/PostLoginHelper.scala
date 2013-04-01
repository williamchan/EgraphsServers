package services.login

import models.frontend.login.LoginViewModel
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.Valid
import services.AppConfig
import services.http.forms.FormConstraints

object PostLoginHelper {
  object keys {
    def email = "loginEmail"
    def password = "loginPassword"
  }

  def formName = "login-form"
  def formConstraints = AppConfig.instance[FormConstraints]
  def form: Form[LoginViewModel] = Form(
    mapping(
      keys.email -> email.verifying(nonEmpty),
      keys.password -> nonEmptyText
    )(LoginViewModel.apply)(LoginViewModel.unapply)
    .verifying("The login and password did not match. Try again?", result => result match {
      case LoginViewModel(email, password) => formConstraints.isValidCustomerAccount(email, password)
    })
  )
}