package controllers.website.admin

import play.data.validation._
import models._
import play.mvc.results.Redirect
import services.mail.Mail
import controllers.WebsiteControllers
import play.mvc.Controller
import play.data.validation.Validation.ValidationResult
import services.blobs.Blobs.Conversions._
import java.io.File
import services.{ImageUtil, Utils}
import org.apache.commons.mail.SimpleEmail
import services.http.{SecurityRequestFilters, AdminRequestFilters, ControllerMethod}

trait PostCelebrityAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters
  protected def adminFilters: AdminRequestFilters
  protected def mail: Mail
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore

  /**
   * First validates inputs, and either redirects with error messages or creates a Celebrity.
   *
   * @return a redirect either to the Create Celebrity page with form errors or to
   *         the created Celebrity's page.
   */
  def postCelebrityAdmin(celebrityId: Long = 0,
                         celebrityEmail: String,
                         celebrityPassword: String,
                         firstName: String,
                         lastName: String,
                         publicName: String,
                         description: String,
                         profileImage: Option[File] = None) = controllerMethod() {

    securityFilters.checkAuthenticity{
        adminFilters.requireAdministratorLogin {admin =>
          val isCreate = (celebrityId == 0)
          val tmp = if (isCreate) new Celebrity() else celebrityStore.findById(celebrityId).get

          val publicNameStr = if (publicName.isEmpty) firstName + " " + lastName else publicName
          val celebrity = celebrityWithValues(tmp, firstName = firstName, lastName = lastName, publicNameStr = publicNameStr, description = description)
          val celebrityUrlSlug = celebrity.urlSlug

          // Account validation, including email and password validations and extant account validations
          val preexistingAccount: Option[Account] = if (isCreate) accountStore.findByEmail(celebrityEmail) else accountStore.findByCelebrityId(celebrityId)
          val passwordValidationOrAccount: Either[ValidationResult, Account] = if (preexistingAccount.isDefined) {
            Right(preexistingAccount.get)
          } else {
            new Account(email = celebrityEmail).withPassword(celebrityPassword)
          }

          if (isCreate) {
            Validation.required("E-mail address", celebrityEmail)
            Validation.email("E-mail address", celebrityEmail)
            Validation.required("Password", celebrityPassword)
            if (preexistingAccount.isDefined) {
              val isUniqueEmail = preexistingAccount.get.celebrityId.isEmpty
              Validation.isTrue("Celebrity with e-mail address already exists", isUniqueEmail)
              if (isUniqueEmail && preexistingAccount.get.password.isDefined) {
                Validation.isTrue("A non-celebrity account with that e-mail already exists. Provide the correct password to turn this account into a celebrity account",
                  preexistingAccount.get.password.get.is(celebrityPassword))
              }
            }
            if (passwordValidationOrAccount.isLeft) {
              Validation.addError("Password", passwordValidationOrAccount.left.get.error.toString)
            }
          }

          Validation.required("Description", description)

          // Name validations
          val isNameRequirementSatisfied = !publicName.isEmpty || (!firstName.isEmpty && !lastName.isEmpty)
          Validation.isTrue("Must provide either Public Name or First and Last Name", isNameRequirementSatisfied)
          if (celebrityUrlSlug.isDefined) {
            val celebrityByUrlSlug = celebrityStore.findByUrlSlug(celebrityUrlSlug.get)
            val isUniqueUrlSlug = if (isCreate) {
              celebrityByUrlSlug.isEmpty
            } else {
              celebrityByUrlSlug.isEmpty || (celebrityByUrlSlug.isDefined && celebrityByUrlSlug.get.id == celebrityId)
            }
            Validation.isTrue("Celebrity with same website name exists. Provide different public name", isUniqueUrlSlug)
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
            redirectWithValidationErrors(celebrityId, celebrityEmail, celebrityPassword, firstName, lastName, publicName, description)

          } else {
            val savedCelebrity = celebrity.save()
            // Celebrity must have been previously saved before saving with assets that live in blobstore
            if (profileImage.isDefined) savedCelebrity.saveWithProfilePhoto(profileImage.get)

            if (isCreate) {

              passwordValidationOrAccount.right.get.copy(celebrityId = Some(savedCelebrity.id)).save()

              // Send the order email
              val email = new SimpleEmail()
              email.setFrom("noreply@egraphs.com", "Egraphs")
              email.addTo(celebrityEmail, publicNameStr)
              email.setSubject("Egraphs Celebrity Account Created")
              email.setMsg(views.Application.email.html.celebrity_created_email(celebrity = savedCelebrity, email = celebrityEmail).toString().trim())
              mail.send(email)
            }

            new Redirect(GetCelebrityAdminEndpoint.url(celebrityId = savedCelebrity.id).url + "?action=preview")
          }
        }
    }
  }

  private def celebrityWithValues(celebrity: Celebrity,
                                  firstName: String,
                                  lastName: String,
                                  publicNameStr: String,
                                  description: String): Celebrity = {
    celebrity.copy(firstName = Utils.toOption(firstName),
      lastName = Utils.toOption(lastName),
      publicName = Utils.toOption(publicNameStr),
      description = Utils.toOption(description))
  }

  private def redirectWithValidationErrors(celebrityId: Long,
                                           celebrityEmail: String,
                                           celebrityPassword: String,
                                           firstName: String,
                                           lastName: String,
                                           publicName: String,
                                           description: String): Redirect = {
    flash.put("celebrityId", celebrityId)
    flash.put("celebrityEmail", celebrityEmail)
    flash.put("celebrityPassword", celebrityPassword)
    flash.put("firstName", firstName)
    flash.put("lastName", lastName)
    flash.put("publicName", publicName)
    flash.put("description", description)
    if (celebrityId == 0) {
      WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityAdminEndpoint.url())
    } else {
      WebsiteControllers.redirectWithValidationErrors(GetCelebrityAdminEndpoint.url(celebrityId = celebrityId))
    }
  }
}
