package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.{SecurityRequestFilters, ControllerMethod}
import play.data.validation.Validation
import controllers.WebsiteControllers
import play.mvc.results.Redirect
import services.db.{TransactionSerializable, DBSession}
import models._

private[controllers] trait PostRegisterEndpoint {
  this: Controller =>

  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore

  def postRegister(name: String, email: String, password: String, password2: String): Any = controllerMethod(openDatabase=false) {

    securityFilters.checkAuthenticity {

      Validation.required("Name", name)
      Validation.required("Email", email)
      Validation.email("Email", email)
      Validation.isTrue("Passwords do not match", password == password2)
      if (!validationErrors.isEmpty) {
        return WebsiteControllers.redirectWithValidationErrors(GetRegisterEndpoint.url())
      }

      val customer = dbSession.connected(TransactionSerializable) {
        val accountOption = accountStore.findByEmail(email)

        Validation.isTrue("Account already exists", !(accountOption.isDefined && accountOption.get.customerId.isDefined))
        if (!validationErrors.isEmpty) {
          return WebsiteControllers.redirectWithValidationErrors(GetRegisterEndpoint.url())
        }

        // Create Account if none exists, and then create Customer.
        val passwordValidationOrAccount = accountOption match {
          case Some(a) => a.withPassword(password)
          case None => {
            Account(email = email).withPassword(password)
          }
        }
        for (passwordValidation <- passwordValidationOrAccount.left) {
          Validation.addError("Password", passwordValidation.error.toString)
        }
        if (!validationErrors.isEmpty) {
          return WebsiteControllers.redirectWithValidationErrors(GetRegisterEndpoint.url())
        }

        val account = passwordValidationOrAccount.right.get
        val customer = Customer(name = name).save()
        account.copy(customerId = Some(customer.id)).save()
        customer
      }

      dbSession.connected(TransactionSerializable) {
        customer.sendNewCustomerEmail()
      }
      session.put(WebsiteControllers.customerIdKey, customer.id.toString)
      new Redirect(Utils.lookupUrl("WebsiteControllers.getRootEndpoint").url)
    }
  }
}
