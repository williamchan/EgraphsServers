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

// TODO: PLAY20 migration. Test and comment this summbitch
class RequireCustomerId @Inject() (customerStore: CustomerStore) {
  
  def apply[A]
    (customerId: Long, parser: BodyParser[A] = parse.anyContent)
    (operation: CustomerRequest[A] => Result)  
    : Action[A] = 
  {
    Action(parser) { request =>
      customerStore.findById(customerId) match {
        case Some(customer) => operation(CustomerRequest(customer, request))
        case None => NotFound("Customer not found")
      }
    }
  }
}
