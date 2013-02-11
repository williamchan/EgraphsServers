package controllers.website

import play.api.mvc._
import services.db.{DBSession, TransactionSerializable}
import models.{Account, CustomerStore, AccountStore}
import services.mail.TransactionalMail
import org.apache.commons.mail.HtmlEmail
import services.http.POSTControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import services.{ConsumerApplication, Utils}
import services.http.filters.HttpFilters
import controllers.routes.WebsiteControllers.getResetPassword
import egraphs.authtoken.AuthenticityToken
import models.frontend.email.{EmailViewModel, ResetPasswordEmailViewModel}
import services.mail.MailUtils
import models.enums.EmailType

private[controllers] trait PostRecoverAccountEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def dbSession: DBSession
  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def httpFilters: HttpFilters  
  protected def transactionalMail: TransactionalMail
  protected def consumerApp: ConsumerApplication

  def postRecoverAccount() = postController() {
    AuthenticityToken.makeAvailable() { implicit authToken =>
      httpFilters.requireAccountEmail.inFlashOrRequest() { account =>
        Action { implicit request =>
          val accountWithResetPassKey = dbSession.connected(TransactionSerializable) {
            account.withResetPasswordKey.save()
          }
          sendRecoveryPasswordEmail(accountWithResetPassKey)
  
          val flashEmail = Utils.getFromMapFirstInSeqOrElse("email", "", request.queryString)
          
          // TODO: Replace this OK with a Redirect to a GET.
          Ok(
            views.html.frontend.simple_message(header = "Success", body ="Instructions for recovering your account have been sent to your email address.")
          ).flashing("email" -> flashEmail)
        }
      }
    }
  }

  /**
   * Sends an email so that the customer can reset password via the getResetPassword endpoint
   */
  private def sendRecoveryPasswordEmail(account: Account)(implicit request: RequestHeader) {
    val emailStack = EmailViewModel(subject = "Egraphs Password Recovery",
                                    fromEmail = "support@egraphs.com",
                                    fromName = "Egraphs Support",
                                    toEmail = account.email)

    val resetPasswordUrl = consumerApp.absoluteUrl(getResetPassword(account.email, account.resetPasswordKey.get).url)
    val resetPasswordEmailStack = ResetPasswordEmailViewModel(email = account.email,
                                                              resetPasswordUrl = resetPasswordUrl)

    transactionalMail.send(emailStack, MailUtils.getResetPasswordTemplateContentParts(EmailType.ResetPassword, resetPasswordEmailStack))
  }
}