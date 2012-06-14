package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import models._
import services.http.POSTControllerMethod
import services.http.forms.{FormSubmissionChecks, FormSubmission}

private[controllers] trait PostLoginEndpoint { this: Controller =>
  import PostLoginEndpoint.PostLoginFormSubmission
  import FormSubmission.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def formChecks: FormSubmissionChecks

  def postLogin = postController() {
    val submission = PostLoginFormSubmission(params.asSubmissionReadable, formChecks)

    submission.errorsOrValidForm match {
      case Left(errors) =>
        submission.redirect(GetLoginEndpoint.url().url)

      case Right(validForm) =>
        session.put(WebsiteControllers.customerIdKey, validForm.customerId)
        new Redirect(Utils.lookupUrl("WebsiteControllers.getRootEndpoint").url)
    }
  }
}

object PostLoginEndpoint {

  /** Resolved product of a fully validated submission */
  case class PostLoginForm(customerId: Long)

  /**
   * Define the fields (and validations thereof) for the form transmitted by this endpoint
   *
   * @param paramsMap either the request parameters or flash parameters converted to Readable
   *     by FormSubmission.Conversions._
   *
   * @param check the checks used by forms in our application.
   **/
  case class PostLoginFormSubmission(paramsMap: FormSubmission.Readable, check: FormSubmissionChecks)
    extends FormSubmission[PostLoginForm]
  {
    //
    // Fields and validations
    //
    val email = new RequiredField[String]("email") {
      def validateIfPresent = {
        for (validEmail <- check.isEmailAddress(stringToValidate).right) yield validEmail
      }
    }

    val password = new RequiredField[String]("password") {
      def validateIfPresent = {
        Right(stringToValidate)
      }
    }

    val customerId = new DerivedField[Long] {
      def validate = {
        for (validEmail <- check.dependentFieldIsValid(email).right;
             validPassword <- check.dependentFieldIsValid(password).right;
             validAccount <- check.isValidAccount(validEmail, validPassword, badCredentialsMessage).right;
             customerId <- check.isCustomerAccount(validAccount, badCredentialsMessage).right)
        yield {
          customerId
        }
      }
    }

    //
    // Valid Form
    //
    protected def formAssumingValid: PostLoginForm = {
      // Safely access the account value in here
      PostLoginForm(customerId.value.get)
    }

    private val badCredentialsMessage = "The username and password did not match. Please try again"
  }

}