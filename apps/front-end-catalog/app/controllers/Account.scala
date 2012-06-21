package controllers

import play.mvc.Controller
import models.frontend.account.AccountSettingsForm
import models.frontend.forms.{FormError, Field}

object Account extends Controller {

  def settings() = {
    request.method match {
      case "POST" => {
        println("POST data")
        println(params.allSimple())
      }
      case _ => {
        val form = AccountSettingsForm(
          fullname = Field(name = "fullname", values = List("Will Chan")),
          username = Field(name = "username", values = List("willchan")),
          email = Field(name = "email", values = List("will@egraphs.com")),
          oldPassword = Field(name = "oldPassword"),
          newPassword = Field(name = "newPassword"),
          passwordConfirm = Field(name = "passwordConfirm"),
          addressLine1 = Field(name = "address.line1", values = List("615 2nd Ave")),
          addressLine2 = Field(name = "address.line2", values = List("Suite 300")),
          city = Field(name = "city", values = List("Seattle")),
          state = Field(name = "state", values = List("WA")),
          postalCode = Field(name = "postalCode", values = List("98102")),
          galleryVisibility = Field(name = "galleryVisibility", values = List("private")),
          notice_stars = Field(name = "notice_stars", values = List("true")),
          generalErrors = List.empty[FormError]
        )
        views.frontend.html.account_settings(form)
      }
    }
  }
}
