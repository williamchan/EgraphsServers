package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import play.data.validation.Validation
import services.db.{DBSession, TransactionSerializable}
import models.{Customer, Account, CustomerStore, AccountStore}
import services.mail.Mail
import org.apache.commons.mail.HtmlEmail
import play.mvc.Router.ActionDefinition
import services.http.POSTControllerMethod

private[controllers] trait PostRecoverAccountEndpoint {
  this: Controller =>

  protected def dbSession: DBSession
  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def mail: Mail

  def postRecoverAccount(email: String) = postController(openDatabase=false) {

    Validation.required("Email", email)
    Validation.email("Email", email)

    val accountOption = dbSession.connected(TransactionSerializable) {
      accountStore.findByEmail(email)
    }
    if (validationErrors.isEmpty) {
      Validation.isTrue("No account exists with that email", accountOption.isDefined && accountOption.get.customerId.isDefined)
    }

    if (!validationErrors.isEmpty) {
      WebsiteControllers.redirectWithValidationErrors(GetRecoverAccountEndpoint.url())

    } else {
      // save Account with new resetPasswordKey (and also gets the Customer so we can write a personalized email)
      val (account, customer) = dbSession.connected(TransactionSerializable) {
        val account = accountOption.get.withResetPasswordKey.save()
        val customer = customerStore.findById(account.customerId.get).get
        (account, customer)
      }

      sendRecoveryPasswordEmail(account, customer)

      flash.put("email", email)
      new Redirect(Utils.lookupUrl("WebsiteControllers.getRecoverAccountConfirmation").url)
    }
  }

  /**
   * Sends an email so that the customer can reset password via the getResetPassword endpoint
   */
  private def sendRecoveryPasswordEmail(account: Account, customer: Customer) {
    val linkActionDefinition: ActionDefinition = GetResetPasswordEndpoint.url(account.id, account.resetPasswordKey.get)
    linkActionDefinition.absolute()
    val email = new HtmlEmail()
    email.setFrom("support@egraphs.com")
    email.addReplyTo("support@egraphs.com")
    email.addTo(account.email)
    email.setSubject("Egraphs Password Recovery")
    email.setMsg(
      views.Application.email.html.reset_password_email(
        customerName = customer.name,
        email = account.email,
        resetPasswordUrl = linkActionDefinition.url
      ).toString().trim()
    )
    mail.send(email)
  }
}
