package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import models._
import services.http.POSTControllerMethod
import com.google.inject.Inject
import services.http.forms.{ReadsFormSubmission, FormSubmissionChecks, FormSubmission}
import controllers.website.PostLoginEndpoint.PostLoginFormSubmissionFactory

private[controllers] trait PostLoginEndpoint { this: Controller =>
  import FormSubmission.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def formChecks: FormSubmissionChecks
  protected def postLoginForms: PostLoginFormSubmissionFactory

  def postLogin = postController() {
    val submission = postLoginForms(params.asSubmissionReadable)

    submission.errorsOrValidForm match {
      case Left(errors) =>
        submission.flashRedirect(GetLoginEndpoint.url().url)

      case Right(validForm) =>
        session.put(WebsiteControllers.customerIdKey, validForm.customerId)
        new Redirect(Utils.lookupUrl("WebsiteControllers.getRootEndpoint").url)
    }
  }
}

object PostLoginEndpoint {

  /** Class to which the fully validated PostLoginFormSubmission resolves */
  case class ValidatedPostLoginForm(customerId: Long)

  /**
   * Define the fields (and validations thereof) for the form transmitted by this endpoint
   *
   * @param paramsMap either the request parameters or flash parameters converted to Readable
   *     by FormSubmission.Conversions._
   *
   * @param check the checks used by forms in our application.
   **/
  case class PostLoginFormSubmission(paramsMap: FormSubmission.Readable, check: FormSubmissionChecks)
    extends FormSubmission[ValidatedPostLoginForm]
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
             validAccount <- check.isValidAccount(validEmail,
                                                  validPassword,
                                                  badCredentialsMessage).right;
             customerId <- check.isCustomerAccount(validAccount, badCredentialsMessage).right)
        yield {
          customerId
        }
      }
    }

    //
    // FormSubmission[ValidatedPostLoginForm] members
    //
    protected def formAssumingValid: ValidatedPostLoginForm = {
      // Safely access the account value in here
      ValidatedPostLoginForm(customerId.value.get)
    }

    //
    // Private members
    //
    private val badCredentialsMessage = "The username and password did not match. Please try again"
  }

  class PostLoginFormSubmissionFactory @Inject()(formChecks: FormSubmissionChecks)
    extends ReadsFormSubmission[PostLoginFormSubmission]
  {
    def apply(readable: FormSubmission.Readable): PostLoginFormSubmission = {
      PostLoginFormSubmission(readable, formChecks)
    }

    def instantiateAgainstReadable(readable: FormSubmission.Readable): PostLoginFormSubmission = {
      apply(readable)
    }
  }

}