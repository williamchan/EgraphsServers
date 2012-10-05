package services.http.filters

import com.google.inject.Inject
import models.CustomerStore
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import services.http.SafePlayParams.Conversions.paramsToOptionalParams
import models.Customer
import models.Account
import play.api.mvc.Results.Redirect
import services.http.EgraphsSession

// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireCustomerLogin @Inject() (customerStore: CustomerStore) {  
  def apply[A](customerId: Long, parser: BodyParser[A] = parse.anyContent)(actionFactory: (Customer, Account) => Action[A])
  : Action[A] = 
  {
    Action(parser) { request =>
      
      val maybeResult = for (customer <- customerStore.findById(customerId)) yield {
        actionFactory(customer, customer.account).apply(request)     
      }
      
      // TODO: PLAY20 migration actually redirect this to the reverse-route of GetLoginCustomerEndpoint
      //   instead of returning  a forbidden.
      maybeResult.getOrElse(noCustomerAccessResult)
    }
  }

  def inSession[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: (Customer, Account) => Action[A])
  : Action[A] = {
    Action(parser) { request =>
      val maybeResult = request.session.getLongOption(EgraphsSession.Key.CustomerId.name).map { customerId =>
        this.apply(customerId, parser)(actionFactory).apply(request)
      }
      
      maybeResult.getOrElse(noCustomerAccessResult)
    }
  }
  
  //
  // Private members
  //
  private val noCustomerAccessResult = Redirect(controllers.routes.WebsiteControllers.getLogin)
}
