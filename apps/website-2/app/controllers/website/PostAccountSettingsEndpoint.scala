package controllers.website

import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import models._
import services.http.{CustomerRequestFilters, POSTControllerMethod}
import services.http.forms.{AccountSettingsForm, AccountSettingsFormFactory, Form}
import play.api.mvc.Action

private[controllers] trait PostAccountSettingsEndpoint { this: Controller =>
  import Form.Conversions._

  protected def postController: POSTControllerMethod
  protected def customerFilters: CustomerRequestFilters
  protected def accountStore: AccountStore
  protected def accountSettingsForms: AccountSettingsFormFactory

  def postAccountSettings() = Action { implicit request =>
    postController() {
      customerFilters.requireCustomerLogin { (customer, account) =>
        // Read a AccountSettingsForm from the params
        val params = request.queryString
        val nonValidatedForm = accountSettingsForms(params.asFormReadable, customer, account)
  
        // Handle valid or error cases
        nonValidatedForm.errorsOrValidatedForm match {
          case Left(errors) =>
            nonValidatedForm.redirectThroughFlash(GetAccountSettingsEndpoint.url().url)
  
          case Right(validForm) =>
            persist(validForm, customer, account)
            new Redirect(GetAccountSettingsEndpoint.url().url)
        }
      }
    }
  }

  private def persist(validForm: AccountSettingsForm.Validated, customer: Customer, account: Account) {
    val (fullname, username, email, newPassword, isGalleryVisible, addressLine1, addressLine2, city, state, postalCode, notice_stars) =
      (validForm.fullname, validForm.username, validForm.email, validForm.newPassword, validForm.galleryVisibility,
        validForm.addressLine1, validForm.addressLine2, validForm.city, validForm.state, validForm.postalCode,
        validForm.noticeStars)

    // Persist Account changes
    account.copy(email = email).save()
    if (!newPassword.isEmpty) {
      account.withPassword(newPassword).right.get.save()
    }

    // Persist Address changes
    val address = account.addresses.headOption match {
      case None => Address(accountId = account.id)
      case Some(a) => a
    }
    address.copy(addressLine1 = addressLine1, addressLine2 = addressLine2, city = city, _state = state, postalCode = postalCode).save()

    // Persist Customer changes
    customer.copy(name = fullname, username = username,
      isGalleryVisible = (isGalleryVisible == "Public"),
      notice_stars = (notice_stars == "on"))
      .save()
  }
}
