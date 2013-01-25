package services.http.filters

import com.google.inject.Inject
import models.{Account, Customer, CustomerStore}
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden
import play.api.mvc.Request

/**
 * Filter for requiring a customer by the id that is in the customer store.
 */
class RequireCustomerId @Inject() (customerStore: CustomerStore) extends Filter[Long, Customer] with RequestFilter[Long, Customer] {

  override def filter(customerId: Long): Either[Result, Customer] = {
    customerStore.findById(customerId).toRight(left = noCustomerIdResult)
  }

  override val form: Form[Long] = Form(
    single(
      "customerId" -> longNumber)
      verifying ("Invalid customerId", {
      case customerId => customerId > 0
    }: Long => Boolean))

  override protected def formFailedResult[A, S >: Source](formWithErrors: Form[Long], source: S)(implicit request: Request[A]): Result = {
    noCustomerIdResult
  }

  def inAccount[A](account: Account, parser: BodyParser[A] = parse.anyContent)(actionFactory: Customer => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      account.customerId match {
        case None => noCustomerIdResult
        case Some(customerId) =>
          val action = this(customerId, parser)(actionFactory)
          action(request)
      }
    }
  }

  def filterInAccount(account: Account): Either[Result, Customer] = {
    for (
      customerId <- account.customerId.toRight(left = noCustomerIdResult).right;
      customer <- this.filter(customerId).right
    ) yield {
      customer
    }
  }

  private val noCustomerIdResult = Forbidden("Valid customer account credentials were required but not provided")
}