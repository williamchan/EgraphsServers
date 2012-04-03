package controllers.website.admin

import play.data.validation._
import models._
import play.mvc.results.Redirect
import services.mail.Mail
import controllers.WebsiteControllers
import play.mvc.Controller
import play.data.validation.Validation.ValidationResult
import play.Logger
import services.blobs.Blobs.Conversions._
import java.io.File
import services.{ImageUtil, Utils}
import org.apache.commons.mail.SimpleEmail
import services.http.{AdminRequestFilters, ControllerMethod}

trait PostCelebrityEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def mail: Mail
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore

  /**
   * First validates inputs, and either redirects with error messages or creates a Celebrity.
   *
   * @return a redirect either to the Create Celebrity page with form errors or to
   *   the created Celebrity's page.
   */
  def postCelebrity(celebrityEmail: String,
                    celebrityPassword: String,
                    firstName: String,
                    lastName: String,
                    publicName: String,
                    description: String,
                    profileImage: Option[File] = None) = controllerMethod() {
    adminFilters.requireAdministratorLogin { adminId =>
      val publicNameStr = if (publicName.isEmpty) firstName + " " + lastName else publicName
      val celebrity = new Celebrity(firstName = Utils.toOption(firstName),
        lastName = Utils.toOption(lastName),
        publicName = Utils.toOption(publicNameStr),
        description = Utils.toOption(description))

      // Account validation, including email and password validations and extant account validations
      Validation.required("E-mail address", celebrityEmail)
      Validation.email("E-mail address", celebrityEmail)
      Validation.required("Password", celebrityPassword)
      val preexistingAccount: Option[Account] = accountStore.findByEmail(celebrityEmail)
      if (preexistingAccount.isDefined) {
        val celebrityDoesNotAlreadyExist = preexistingAccount.get.celebrityId.isEmpty
        Validation.isTrue("Celebrity with e-mail address already exists", celebrityDoesNotAlreadyExist)
        if (celebrityDoesNotAlreadyExist && preexistingAccount.get.password.isDefined) {
          Validation.isTrue("A non-celebrity account with that e-mail already exists. Provide the correct password to turn this account into a celebrity account", preexistingAccount.get.password.get.is(celebrityPassword))
        }
      }
      val passwordValidationOrAccount: Either[ValidationResult, Account] = if (preexistingAccount.isDefined) {
        Right(preexistingAccount.get)
      } else {
        new Account(email = celebrityEmail).withPassword(celebrityPassword)
      }
      if (passwordValidationOrAccount.isLeft) {
        Validation.addError("Password", passwordValidationOrAccount.left.get.error.toString)
      }

      Validation.required("Description", description)

      // Name validations
      val isNameRequirementSatisfied = !publicName.isEmpty || (!firstName.isEmpty && !lastName.isEmpty)
      Validation.isTrue("Must provide either Public Name or First and Last Name", isNameRequirementSatisfied)
      val celebrityUrlSlug = celebrity.urlSlug
      if (celebrityUrlSlug.isDefined) {
        Validation.isTrue("Celebrity with same website name exists. Provide different public name", celebrityStore.findByUrlSlug(celebrityUrlSlug.get).isEmpty)
      }

      // Profile image validations
      if (profileImage.isDefined) {
        val dimensions = ImageUtil.getDimensions(profileImage.get)
        if (dimensions.isEmpty) {
          Validation.addError("Profile Photo", "No image found for Profile Photo")
        } else {
          val resolutionStr = dimensions.get.width + ":" + dimensions.get.height
          Validation.isTrue("Profile Photo must be 200x200 - resolution was " + resolutionStr, dimensions.get.width == 200 && dimensions.get.height == 200)
        }
      }

      if (!validationErrors.isEmpty) {
        WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityEndpoint.url())

      } else {
        // Persist Celebrity
        Logger.info("Creating celebrity")
        val savedCelebrity = celebrity.save()
        if (profileImage.isDefined) savedCelebrity.saveWithProfilePhoto(profileImage.get)
        passwordValidationOrAccount.right.get.copy(celebrityId = Some(savedCelebrity.id)).save()

        // Send the order email
        val email = new SimpleEmail()
        email.setFrom("noreply@egraphs.com", "Egraphs")
        email.addTo(celebrityEmail, publicNameStr)
        email.setSubject("Egraphs Celebrity Account Created")
        email.setMsg(views.Application.email.html.celebrity_created_email(celebrity = savedCelebrity, email = celebrityEmail).toString().trim())
        mail.send(email)

        new Redirect(WebsiteControllers.lookupGetCelebrity(celebrityUrlSlug.get).url)
      }
    }
  }
}
