package services.http.filters

import com.google.inject.Inject

import models.AccountStore
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.data.Form
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.NotFound
import play.api.mvc.BodyParser
import play.api.mvc.Action
import play.api.mvc.Result
import services.http.AccountRequest


// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireAccountEmail @Inject() (accountStore: AccountStore) {
  def apply[A](email: String, parser: BodyParser[A] = parse.anyContent)(operation: AccountRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { request =>
      accountStore.findByEmail(email) match {
        case Some(account) => operation(AccountRequest(account, request))
        case None => NotFound("Account not found")
      }
    }
  }

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(operation: AccountRequest[A] => Result): Action[A] = {
    Action(parser) { implicit request =>
      Form(single("email" -> text)).bindFromRequest.fold(
        errors => BadRequest("Email was required but not provided"),
        email => this.apply(email, parser)(operation)(request)
      )
    }
  }

  def inFlashOrRequest[A](parser: BodyParser[A] = parse.anyContent)(operation: AccountRequest[A] => Result): Action[A] = {
    Action(parser) { request =>
      // flash takes precedence over request args
      val maybeEmail = request.flash.get("email")
      
      maybeEmail match {
        case None =>
          this.inRequest(parser)(operation)(request)
        case Some(email) =>
          this.apply(email, parser)(operation)(request)
      }
    }
  }
}
