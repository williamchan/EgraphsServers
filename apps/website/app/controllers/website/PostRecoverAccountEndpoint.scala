package controllers.website

import play.api.mvc._
import services.db.{DBSession, TransactionSerializable}
import models.Account
import org.apache.commons.mail.HtmlEmail
import services.http.POSTControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import services.Utils
import services.http.filters.HttpFilters
import controllers.routes.WebsiteControllers.getResetPassword
import egraphs.authtoken.AuthenticityToken
import models.frontend.email.{EmailViewModel, ResetPasswordEmailViewModel}
import services.mail.MailUtils
import models.enums.EmailType
import services.email.ResetPasswordEmail

private[controllers] trait PostRecoverAccountEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def dbSession: DBSession
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters  

  def postRecoverAccount() = postController() {
    AuthenticityToken.makeAvailable() { implicit authToken =>
      httpFilters.requireAccountEmail.inFlashOrRequest() { account =>
        Action { implicit request =>
          val accountWithResetPassKey = dbSession.connected(TransactionSerializable) {
            account.withResetPasswordKey.save()
          }

          ResetPasswordEmail(account = accountWithResetPassKey).send()
          val flashEmail = Utils.getFromMapFirstInSeqOrElse("email", "", request.queryString)
          
          // TODO: Replace this OK with a Redirect to a GET.
          Ok(
            views.html.frontend.simple_message(header = "Success", body ="Instructions for recovering your account have been sent to your email address.")
          ).flashing("email" -> flashEmail)
        }
      }
    }
  }
}