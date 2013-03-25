package services.login

import egraphs.playutils.FlashableForm._
import models._
import models.frontend.login_page.RegisterConsumerViewModel
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.Valid
import play.api.mvc._
import play.api.mvc.Results.Redirect
import services.AppConfig
import services.db.{TransactionReadCommitted, TransactionSerializable, DBSession}
import services.email.AccountCreationEmail
import services.http.forms.FormConstraints
import services.mail.BulkMailList

trait PostRegisterHelper {

  protected def bulkMailList: BulkMailList
  protected def dbSession: DBSession

  protected def redirectOrCreateAccountCustomerTuple(implicit request: Request[AnyContent])
  : Either[Result, (Account, Customer)] = {

    dbSession.connected(TransactionSerializable) {
      val (form, formName) = (PostRegisterHelper.form, PostRegisterHelper.formName)

      form.bindFromRequest.fold(
        formWithErrors => Left(Redirect(controllers.routes.WebsiteControllers.getLogin()).flashingFormData(formWithErrors, formName)),
        validForm => Right(newAccountAndCustomerFromEmailAndPassword(validForm.registerEmail, validForm.registerPassword, validForm.bulkEmail))
      )
    }
  }

  protected def newAccountAndCustomerFromEmailAndPassword(email: String, password: String, bulkEmail: Boolean): (Account, Customer) = {
	// The form validation already told us we can add this fella to the DB
	val passwordErrorOrAccount = Account(email = email).withPassword(password)
	val unsavedAccount = passwordErrorOrAccount.right.getOrElse(
	  throw new RuntimeException("The password provided by registering user " +
	    email + "somehow passed validation but failed while setting onto the account"))

	// We don't require a name to register so...screw it his name is the first part of his email.
	val customerName = email.split("@").head
	val savedCustomer = unsavedAccount.createCustomer(customerName).copy(notice_stars = bulkEmail).save()
	val savedAccount = unsavedAccount.copy(customerId = Some(savedCustomer.id)).withResetPasswordKey.save()

	(savedAccount, savedCustomer)
  }

  protected def newCustomerTasks(account: Account, customer: Customer) = {
    // We'll add the new account to our bulk mailing list if they said so.
    if (customer.notice_stars) {
      bulkMailList.subscribeNewAsync(account.email)
    }
    // Shoot out a welcome email
    dbSession.connected(TransactionReadCommitted) {
      AccountCreationEmail(account = account, verificationNeeded = true).send()
    }
  }
}

object PostRegisterHelper {
  def formName = "register-form"
  def formConstraints = AppConfig.instance[FormConstraints]

  def form: Form[RegisterConsumerViewModel] = Form(mapping(
    "registerEmail" -> email.verifying(nonEmpty, formConstraints.isValidNewCustomerEmail),
    "registerPassword" -> text.verifying(nonEmpty, formConstraints.isPasswordValid),
    "bulk-email" -> boolean)(RegisterConsumerViewModel.apply)(RegisterConsumerViewModel.unapply)
  )
}