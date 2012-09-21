package services.http.filters

import com.google.inject.Inject
import models.Account
import models.AccountStore
import play.api.mvc.Action
import play.api.mvc.Results.{NotFound, Forbidden, Redirect}
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden
import play.api.mvc.WrappedRequest
import models.AdministratorStore
import controllers.WebsiteControllers
import services.http.SafePlayParams.Conversions._
import services.http.AdminRequest

// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireAdministratorLogin @Inject() (adminStore: AdministratorStore) {  
  def apply[A](adminId: Long, parser: BodyParser[A] = parse.anyContent)(operation: AdminRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { request =>     
      val maybeResult = for (
        admin <- adminStore.findById(adminId)
      ) yield {
        operation(AdminRequest(admin, request))
      }
      
      // TODO: PLAY20 migration actually redirect this to the reverse-route of GetLoginAdminEndpoint
      //   instead of returning  a forbidden.
      maybeResult.getOrElse(noAdminAccessResult)
    }
  } 

  def inSession[A](parser: BodyParser[A] = parse.anyContent)(operation: AdminRequest[A] => Result)
  : Action[A] = {
    Action(parser) { request =>
      val maybeResult = request.session.getLongOption(WebsiteControllers.adminIdKey).map { adminId =>
        this.apply(adminId, parser)(operation)(request)
      }
      
      maybeResult.getOrElse(noAdminAccessResult)
    }
  }
  
  //
  // Private members
  //
  private val noAdminAccessResult = Forbidden("This feature requires admin access")
}
