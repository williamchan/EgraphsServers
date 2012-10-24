package services.http.filters

import com.google.inject.Inject

import models.Account
import models.AccountStore
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.NotFound

// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireAccountEmail @Inject() (accountStore: AccountStore) {
  def apply[A](email: String, parser: BodyParser[A] = parse.anyContent)(actionFactory: Account => Action[A])
  : Action[A] = 
  {
    Action(parser) { request =>
      accountStore.findByEmail(email) match {
        case Some(account) => actionFactory(account).apply(request)
        case None => NotFound("Account not found")
      }
    }
  }

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: Account => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      Form(single("email" -> text)).bindFromRequest.fold(
        errors => BadRequest("Email was required but not provided"),
        email => this.apply(email, parser)(actionFactory)(request)
      )
    }
  }

  def inFlashOrRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: Account => Action[A]): Action[A] = {
    Action(parser) { request =>
      // flash takes precedence over request args
      val maybeEmail = request.flash.get("email")
      
      maybeEmail match {
        case None =>
          this.inRequest(parser)(actionFactory)(request)
        case Some(email) =>
          this.apply(email, parser)(actionFactory)(request)
      }
    }
  }
}
