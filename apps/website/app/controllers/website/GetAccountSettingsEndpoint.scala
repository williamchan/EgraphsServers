package controllers.website

import models.frontend.account.{AccountSettingsForm => AccountSettingsFormView}
import models.frontend.forms.Field
import models.frontend.forms.FormError
import models.Account
import models.Customer
import play.api.mvc._
import play.api._
import services.http.forms.AccountSettingsForm.Fields
import services.http.forms.AccountSettingsFormFactory
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import services.http.filters.HttpFilters

private[controllers] trait GetAccountSettingsEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters  
  protected def accountSettingsForms: AccountSettingsFormFactory

  def getAccountSettings = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireCustomerLogin.inSession() { case (customer, account) =>
      Action { implicit request =>
        val form = makeFormView(customer, account, request.flash)
  
        val displayableErrors = (List(form.fullname.error, form.username.error, form.email.error,
          form.oldPassword.error, form.newPassword.error, form.passwordConfirm.error,
          form.addressLine1.error,form.addressLine2.error, form.city.error, form.state.error, form.postalCode.error,
          form.galleryVisibility.error, form.notice_stars.error) ::: form.generalErrors.toList)
          .asInstanceOf[List[Option[FormError]]].filter(e => e.isDefined).map(e => e.get.description)
  
        Ok(views.html.frontend.account_settings(form=form, displayableErrors))
      }
    }
  }

  private def makeFormView(customer: Customer, account: Account, flash: Flash): AccountSettingsFormView = {
    // Get form from flash if possible
    val maybeFormData = accountSettingsForms.getFormReader(customer, account).read(flash.asFormReadable).map { form =>
      val nonFieldSpecificErrors = form.fieldInspecificErrors.map(error => error.asViewError)

      // TODO: asViewField should probably handle these... where does weird string come from
      val stateViewField = form.state.value.get match {
        case Some("? string: ?") => form.state.asViewField.copy(values = List(""))
        case _ => form.state.asViewField
      }
      val noticeStarsViewField = form.noticeStars.value.get match {
        case Some("on") => form.noticeStars.asViewField.copy(values = List("true")) // "on" comes from checkbox
        case _ => form.noticeStars.asViewField.copy(values = List("false"))         // value is missing if checkbox is unchecked
      }
      AccountSettingsFormView(
        fullname = form.fullname.asViewField,
        username = form.username.asViewField,
        email = form.email.asViewField,
        oldPassword = form.oldPassword.asViewField,
        newPassword = form.newPassword.asViewField,
        passwordConfirm = form.passwordConfirm.asViewField,
        addressLine1 = form.addressLine1.asViewField,
        addressLine2 = form.addressLine2.asViewField,
        city = form.city.asViewField,
        state = stateViewField,
        postalCode = form.postalCode.asViewField,
        galleryVisibility = form.galleryVisibility.asViewField,
        notice_stars = noticeStarsViewField,
        generalErrors = nonFieldSpecificErrors
      )
    }

    // If we couldn't find the form in the flash we'll just make an empty form with the right names
    maybeFormData.getOrElse {
      val (addressLine1, addressLine2, city, state, postalCode) = account.addresses.headOption match {
        case None => ("", "", "", "", "")
        case Some(address) => (address.addressLine1, address.addressLine2.getOrElse(""), address.city, address._state, address.postalCode)
      }
      AccountSettingsFormView(
        fullname = Field(name = Fields.Fullname.name, values = List(customer.name)),
        username = Field(name = Fields.Username.name, values = List(customer.username)),
        email = Field(name = Fields.Email.name, values = List(account.email)),
        oldPassword = Field(name = Fields.OldPassword.name),
        newPassword = Field(name = Fields.NewPassword.name),
        passwordConfirm = Field(name = Fields.PasswordConfirm.name),
        addressLine1 = Field(name = Fields.AddressLine1.name, values = List(addressLine1)),
        addressLine2 = Field(name = Fields.AddressLine2.name, values = List(addressLine2)),
        city = Field(name = Fields.City.name, values = List(city)),
        state = Field(name = Fields.State.name, values = List(state)),
        postalCode = Field(name = Fields.PostalCode.name, values = List(postalCode)),
        galleryVisibility = Field(name = Fields.GalleryVisibility.name, values = List(if (customer.isGalleryVisible) "public" else "private")),
        notice_stars = Field(name = Fields.NoticeStars.name, values = List(customer.notice_stars.toString)),
        generalErrors = List.empty[FormError]
      )
    }
  }
}

object GetAccountSettingsEndpoint {

  def url: String = {
    controllers.routes.WebsiteControllers.getAccountSettings().url
  }
}
