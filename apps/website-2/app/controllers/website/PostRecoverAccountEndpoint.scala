package controllers.website

import play.api.mvc.Controller
import services.db.{DBSession, TransactionSerializable}
import models.{Customer, Account, CustomerStore, AccountStore}
import services.mail.TransactionalMail
import org.apache.commons.mail.HtmlEmail
import services.http.{SafePlayParams, AccountRequestFilters, POSTControllerMethod}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait PostRecoverAccountEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>
  import SafePlayParams.Conversions._

  protected def dbSession: DBSession
  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def accountRequestFilters: AccountRequestFilters
  protected def transactionalMail: TransactionalMail

  def postRecoverAccount() = postController() {
    accountRequestFilters.requireValidAccountEmail(request.params.getOption("email").getOrElse("Nothing")) {
      account =>

        val (customer, accountWithResetPassKey) = dbSession.connected(TransactionSerializable) {
          val accountWithResetPassKey = account.withResetPasswordKey.save()
          (customerStore.get(account.customerId.get), accountWithResetPassKey)
        }
        sendRecoveryPasswordEmail(accountWithResetPassKey, customer)

        val flash = play.mvc.Http.Context.current().flash()
        flash.put("email", request.params.getOption("email").getOrElse(""))

        views.html.frontend.simple_confirmation(header = "Success", body ="Instructions for recovering your account have been sent to your email address.")
    }
  }

  /**
   * Sends an email so that the customer can reset password via the getResetPassword endpoint
   */
  private def sendRecoveryPasswordEmail(account: Account, customer: Customer) {
    val email = new HtmlEmail()
    email.setFrom("support@egraphs.com")
    email.addReplyTo("support@egraphs.com")
    email.addTo(account.email)
    email.setSubject("Egraphs Password Recovery")
    email.setMsg(
      views.html.Application.email.reset_password_email(
        customerName = customer.name,
        email = account.email,
        resetPasswordUrl = GetResetPasswordEndpoint.absoluteUrl(account.email, account.resetPasswordKey.get)
      ).toString().trim()
    )
    transactionalMail.send(email)
  }
}
