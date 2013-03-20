package controllers.website.consumer

import play.api.data.Form
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import play.api.mvc._
import play.api.mvc.Results.{Ok, Redirect}
import services.http.{WithoutDBConnection, EgraphsSession, POSTControllerMethod}
import services.http.EgraphsSession.Key._
import models._
import services.mvc.ImplicitHeaderAndFooterData
import controllers.WebsiteControllers
import services.db.{TransactionReadCommitted, TransactionSerializable, DBSession}
import play.api.mvc.Results.Redirect
import play.api.mvc.Request
import play.api.mvc.AnyContent
import services.logging.Logging
import services.http.EgraphsSession.Conversions._
import services.email.AccountCreationEmail
import services.mail.BulkMailList
import egraphs.playutils.FlashableForm._
import models.frontend.login_page.RegisterConsumerViewModel
import services.AppConfig
import services.http.forms.FormConstraints
import services.http.EgraphsSession

/**
 * The POST target for creating a new account at egraphs.
 */
private[controllers] trait PostRegisterConsumerEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  //
  // Services
  //
  protected def postController: POSTControllerMethod
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def dbSession: DBSession

  //
  // Controllers
  //
  def postRegisterConsumerEndpoint = postController() {
    Action { implicit request =>
      val redirects = for(
        // Get either the account and customer or a redirect back to the sign-in page
        accountAndCustomer <- redirectOrCreateAccountCustomerTuple(request).right
      ) yield {
        // OK We made it! The user is created. Unpack account and customer
        val (account, customer) = accountAndCustomer

        // We'll automatically add the new account to our bulk mailing list
        if (customer.notice_stars) {
          bulkMailList.subscribeNewAsync(account.email)
        }
  
        // Shoot out a welcome email
        dbSession.connected(TransactionReadCommitted) {
          AccountCreationEmail(account = account, verificationNeeded = true).send()
        }

        // Find out whether the user is creating an account to complete their celebrity request
        val redirectCall: Call = request.session.requestedStarRedirectOrCall(
          customer.id,
          controllers.routes.WebsiteControllers.getAccountSettings)

        Redirect(redirectCall).withSession(
          request.session
            .withCustomerId(customer.id)
            .withUsernameChanged
            .removeRequestedStar
            .removeRequestStarTargetUrl
        ).withCookies(Cookie(HasSignedUp.name, true.toString, maxAge = Some(EgraphsSession.COOKIE_MAX_AGE)))
      }
      redirects.merge
    }
  }

  private def redirectOrCreateAccountCustomerTuple(implicit request: Request[AnyContent])
  : Either[Result, (Account, Customer)] = {

    dbSession.connected(TransactionSerializable) {
      val form = PostRegisterConsumerEndpoint.form

      form.bindFromRequest.fold(
        formWithErrors => {
          Left(Redirect(controllers.routes.WebsiteControllers.getLogin()).flashingFormData(formWithErrors))
        }
        , validForm => {
          // The form validation already told us we can add this fella to the DB
          val passwordErrorOrAccount = Account(email = validForm.email).withPassword(validForm.password)
          val unsavedAccount = passwordErrorOrAccount.right.getOrElse(
            throw new RuntimeException("The password provided by registering user " +
              validForm.email + "somehow passed validation but failed while setting onto the account"))

          // We don't require a name to register so...screw it his name is the first part of
          // his email.
          val customerName = validForm.email.split("@").head
          val savedCustomer = unsavedAccount.createCustomer(customerName).copy(notice_stars = validForm.bulkEmail).save()
          val savedAccount = unsavedAccount.copy(customerId = Some(savedCustomer.id)).withResetPasswordKey.save()

          Right(savedAccount, savedCustomer)
        }
      ) 
    }
  }
}

object PostRegisterConsumerEndpoint {
  def formConstraints = AppConfig.instance[FormConstraints]

  def form: Form[RegisterConsumerViewModel] = Form(mapping(
    "email" -> email.verifying(nonEmpty, formConstraints.isValidNewCustomerEmail),
    "password" -> text.verifying(nonEmpty, formConstraints.isPasswordValid),
    "bulk-email" -> boolean)(RegisterConsumerViewModel.apply)(RegisterConsumerViewModel.unapply)
  )
}
