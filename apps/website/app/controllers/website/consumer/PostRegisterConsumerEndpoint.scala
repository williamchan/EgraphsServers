package controllers.website.consumer

import play.api.mvc._
import play.api.mvc.Results.Redirect
import services.http.POSTControllerMethod
import services.http.EgraphsSession.Key._
import services.mvc.ImplicitHeaderAndFooterData
import services.http.EgraphsSession
import services.http.EgraphsSession.Conversions._
import services.login.PostRegisterHelper

/**
 * The POST target for creating a new account at Egraphs.
 */
private[controllers] trait PostRegisterConsumerEndpoint extends PostRegisterHelper with ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def postController: POSTControllerMethod

  def postRegisterConsumerEndpoint = postController() {
    Action { implicit request =>

      val redirects = for(
        // Get either the account and customer or a redirect back to the sign-in page
        accountAndCustomer <- redirectOrCreateAccountCustomerTuple(request).right
      ) yield {
        // OK We made it! The user is created. Unpack account and customer
        val (account, customer) = accountAndCustomer

        // Send welcome email and perform any other necessary new account tasks
        newCustomerTasks(account, customer)

        // Find out whether the user is creating an account to complete their celebrity request
        val redirectCall: Call = request.session.requestedStarRedirectOrCall(
          customer.id,
          account.email,
          controllers.routes.WebsiteControllers.getAccountSettings)

        Redirect(redirectCall).withSession(
          request.session
            .withCustomerId(customer.id)
            .withUsernameChanged
            .removeRequestedStar
            .removeAfterLoginRedirectUrl
        ).withCookies(Cookie(HasSignedUp.name, true.toString, maxAge = Some(EgraphsSession.COOKIE_MAX_AGE)))
      }
      redirects.merge
    }
  }
}
