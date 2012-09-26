package services.http.filters

import services.http.CustomerRequest
import models.CustomerStore
import com.google.inject.Inject
import models.Account
import play.api.mvc.WrappedRequest
import play.api.mvc.Action
import play.api.mvc.Request
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import models.Customer
import services.http.SafePlayParams.Conversions._
import services.http.EgraphsSession
import play.api.mvc.Results.Redirect

// TODO: PLAY20 migration. Test and comment this summbitch
class RequireCustomerId @Inject() (customerStore: CustomerStore) {
  
  def apply[A]
    (customerId: Long, parser: BodyParser[A] = parse.anyContent)
    (actionFactory: Customer => Action[A])  
    : Action[A] = 
  {
    Action(parser) { request =>
      customerStore.findById(customerId) match {
        case Some(customer) => actionFactory(customer).apply(request)
        case None => NotFound("Customer not found")
      }
    }
  }

  /** 
   * If the customerId is in the session this implies they are logged in.
   */
  def inSession[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: Customer => Action[A]): Action[A] = 
  {
    Action(parser) { request =>
      val maybeResult = request.session.getLongOption(EgraphsSession.Key.CustomerId.name).map { customerId =>
        this.apply(customerId, parser)(actionFactory)(request)
      }
      
      maybeResult.getOrElse(notLoggedInResult)
    }
  }

  private val notLoggedInResult = Redirect(controllers.routes.WebsiteControllers.getLogin).withNewSession
}
