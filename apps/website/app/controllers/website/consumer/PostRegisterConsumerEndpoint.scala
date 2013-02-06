package controllers.website.consumer

import play.api.mvc.{Action, Controller, Result}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.{WithoutDBConnection, EgraphsSession, POSTControllerMethod}
import models._
import services.mvc.ImplicitHeaderAndFooterData
import services.http.forms.purchase.FormReaders
import services.http.forms.Form
import controllers.WebsiteControllers
import services.db.{TransactionReadCommitted, TransactionSerializable, DBSession}
import play.api.mvc.Results.Redirect
import Form.Conversions._
import play.api.mvc.Request
import play.api.mvc.AnyContent
import services.ConsumerApplication
import services.logging.Logging
import services.http.EgraphsSession.Conversions._

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
  protected def formReaders: FormReaders
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def dbSession: DBSession
  protected def consumerApp: ConsumerApplication

  //
  // Controllers
  //
  def postRegisterConsumerEndpoint = postController(dbSettings = WithoutDBConnection) {
    Action { implicit request =>
      val redirects = for(
        // Get either the account and customer or a redirect back to the sign-in page
        accountAndCustomer <- redirectOrCreateAccountCustomerTuple(request).right
      ) yield {
        // OK We made it! The user is created. Unpack account and customer
        val (account, customer) = accountAndCustomer
  
        // Shoot out a welcome email
        dbSession.connected(TransactionReadCommitted) {
          Customer.sendNewCustomerEmail(
            account = account, 
            verificationNeeded = true, 
            mail = customer.services.mail, 
            consumerApp = consumerApp
           )
        }

        Redirect(controllers.routes.WebsiteControllers.getAccountSettings).withSession(
          request.session
            .withCustomerId(customer.id)
            .withUsernameChanged
            .withHasSignedUp
        )
      }

      redirects.merge
    }
  }

  //
  // Private members
  //
  private def redirectOrCreateAccountCustomerTuple(request: Request[AnyContent])
  : Either[Result, (Account, Customer)] = 
  {
    dbSession.connected(TransactionSerializable) {      
      val formReadableParams = request.asFormReadable
      val registrationReader = formReaders.forRegistrationForm
      val registrationForm = registrationReader.instantiateAgainstReadable(formReadableParams)

      for (
        validForm <- registrationForm.errorsOrValidatedForm.left.map {errors =>
                       val failRedirectUrl = controllers.routes.WebsiteControllers.getLogin().url
                       registrationForm.redirectThroughFlash(failRedirectUrl)(request.flash)
                     }.right
      ) yield {
        // The form validation already told us we can add this fella to the DB
        val passwordErrorOrAccount = Account(email=validForm.email).withPassword(validForm.password)
        val unsavedAccount = passwordErrorOrAccount.right.getOrElse(
          throw new RuntimeException("The password provided by registering user " +
            validForm.email + "somehow passed validation but failed while setting onto the account"
          )
        )

        // We don't require a name to register so...screw it his name is the first part of
        // his email.
        val customerName = validForm.email.split("@").head
        val savedCustomer = unsavedAccount.createCustomer(customerName).save()
        val savedAccount = unsavedAccount.copy(customerId=Some(savedCustomer.id)).withResetPasswordKey.save()

        (savedAccount, savedCustomer)
      }
    }
  }
}

object PostRegisterConsumerEndpoint extends Logging