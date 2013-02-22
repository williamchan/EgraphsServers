package controllers.website

import play.api.mvc.Controller
import play.api.mvc.Action
import controllers.routes.WebsiteControllers.getAccountSettings
import models._
import services.http.filters.HttpFilters
import services.http.POSTControllerMethod
import services.http.forms.{AccountSettingsForm, AccountSettingsFormFactory, Form}
import services.mail.BulkMailList

private[controllers] trait PostAccountSettingsEndpoint { this: Controller =>
  import Form.Conversions._

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def accountSettingsForms: AccountSettingsFormFactory
  protected def bulkMailList: BulkMailList

  def postAccountSettings() = postController() {
    httpFilters.requireCustomerLogin.inSession() { case (customer, account) =>
      Action { implicit request =>
        implicit val flash = request.flash

        // Read a AccountSettingsForm from the params
        val params = request.queryString
        val nonValidatedForm = accountSettingsForms(request.asFormReadable, customer, account)

        // Handle valid or error cases
        nonValidatedForm.errorsOrValidatedForm match {
          case Left(errors) =>
            nonValidatedForm.redirectThroughFlash(getAccountSettings().url)

          case Right(validForm) =>
            persist(validForm, customer, account)
            Redirect(getAccountSettings)
        }
      }
    }
  }

  private def persist(validForm: AccountSettingsForm.Validated, customer: Customer, account: Account) {
    val (fullname, username, email, newPassword, isGalleryVisible, notice_stars) =
      (validForm.fullname, validForm.username, validForm.email, validForm.newPassword, validForm.galleryVisibility,
        validForm.noticeStars)

    // Persist Account changes
    account.copy(email = email).save()
    if (!newPassword.isEmpty) {
      account.withPassword(newPassword).right.get.save()
    }

    // Subscribe or unsubscribe from the mailing list
    if (!customer.notice_stars && notice_stars == "on") {
      bulkMailList.subscribeNewAsync(email)
    } else if (customer.notice_stars && notice_stars == "off") {
      bulkMailList.removeMember(email)
    }

    // Persist Customer changes
    customer.copy(name = fullname, username = username,
      isGalleryVisible = (isGalleryVisible == "Public"),
      notice_stars = (notice_stars == "on"))
      .save()
  }
}
