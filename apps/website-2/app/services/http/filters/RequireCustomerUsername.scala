package services.http.filters

import com.google.inject.Inject

import models.{Customer, CustomerStore}
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import models.Customer

// TODO: PLAY20 migration. Test and comment this summbitch
class RequireCustomerUsername @Inject() (customerStore: CustomerStore) {
  
  def apply[A]
    (username: String, parser: BodyParser[A] = parse.anyContent)
    (actionFactory: Customer => Action[A])  
    : Action[A] = 
  {
    Action(parser) { request =>
      customerStore.findByUsername(username) match {
        case Some(customer) => actionFactory(customer).apply(request)
        case None => NotFound("Customer not found")
      }
    }
  }
}
