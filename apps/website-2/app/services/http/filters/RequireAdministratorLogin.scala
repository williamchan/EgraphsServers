package services.http.filters

import com.google.inject.Inject
import controllers.WebsiteControllers
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden
import services.http.SafePlayParams.Conversions.paramsToOptionalParams
import models.Administrator
import models.AdministratorStore
import models.Account
import models.AccountStore
import services.http.EgraphsSession

// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireAdministratorLogin @Inject() (adminStore: AdministratorStore, accountStore: AccountStore) {  
  
  def apply[A](adminId: Long, parser: BodyParser[A] = parse.anyContent)
  (actionFactory: (Administrator, Account) => Action[A])
  : Action[A] = 
  {
    Action(parser) { request =>     
      val maybeResult = for (
        admin <- adminStore.findById(adminId);
        account <- accountStore.findByAdministratorId(adminId)
      ) yield {
        actionFactory(admin, account).apply(request)      
      }
      
      // TODO: PLAY20 migration actually redirect this to the reverse-route of GetLoginAdminEndpoint
      //   instead of returning  a forbidden.
      maybeResult.getOrElse(noAdminAccessResult)
    }
  } 

  def inSession[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: (Administrator, Account) => Action[A])
  : Action[A] = {
    Action(parser) { request =>
      val maybeResult = request.session.getLongOption(EgraphsSession.Key.AdminId.name).map { adminId =>
        this.apply(adminId, parser)(actionFactory).apply(request)
      }
      
      maybeResult.getOrElse(noAdminAccessResult)
    }
  }
  
  //
  // Private members
  //
  private val noAdminAccessResult = Forbidden("This feature requires admin access")
}
