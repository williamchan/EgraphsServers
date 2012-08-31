package controllers.website.admin

import play.data.validation._
import models._
import enums.PublishedStatus
import play.mvc.results.Redirect
import services.mail.TransactionalMail
import controllers.WebsiteControllers
import play.mvc.Controller
import play.data.validation.Validation.ValidationResult
import services.blobs.Blobs.Conversions._
import java.io.File
import services.{ImageUtil, Utils}
import services.http.{POSTControllerMethod, AdminRequestFilters}
import org.apache.commons.mail.HtmlEmail

trait PostCelebrityAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def transactionalMail: TransactionalMail
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore

  /**
   * This code has become Frankenstein. Refactor to use services.http.forms.Form
   *
   * First validates inputs, and either redirects with error messages or creates a Celebrity.
   *
   * @return a redirect either to the Create Celebrity page with form errors or to
   *         the created Celebrity's page.
   */
  def postCelebrityAdmin(celebrityId: Long = 0,
                         celebrityEmail: String,
                         celebrityPassword: String,
                         publicName: String,
                         publishedStatusString: String,
                         bio: String,
                         casualName: String,
                         organization: String,
                         roleDescription: String,
                         twitterUsername: String,
                         profileImage: Option[File] = None,
                         landingPageImage: Option[File] = None,
                         logoImage: Option[File] = None) = postController() {
      adminFilters.requireAdministratorLogin { admin =>
        val isCreate = (celebrityId == 0)
        val tmp = if (isCreate) new Celebrity() else celebrityStore.get(celebrityId)

        val celebrity = celebrityWithValues(tmp, publicName = publicName,
          bio = bio, casualName = casualName, organization = organization, roleDescription = roleDescription, twitterUsername = twitterUsername)
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

        Validation.required("Public Name", publicName)
        Validation.required("Short Bio", bio)
        Validation.required("Organization", organization)

        Validation.isTrue("Public Name has maximum length of 128", publicName.length < 128)            // column width in database
        Validation.isTrue("Organization has maximum length of 128", organization.length < 128)         // column width in database
        Validation.isTrue("Role Description has maximum length of 128", roleDescription.length < 128)  // column width in database

        // Name validations
//        if (celebrityUrlSlug.isDefined) {
          val celebrityByUrlSlug = celebrityStore.findByUrlSlug(celebrityUrlSlug)
          val isUniqueUrlSlug = if (isCreate) {
            celebrityByUrlSlug.isEmpty
          } else {
            celebrityByUrlSlug.isEmpty || (celebrityByUrlSlug.isDefined && celebrityByUrlSlug.get.id == celebrityId)
          }
          Validation.isTrue("Celebrity with same website name exists. Provide different public name", isUniqueUrlSlug)
//        }

        // Profile image validations
        if (profileImage.isDefined) {
          val dimensions = ImageUtil.getDimensions(profileImage.get)
          if (dimensions.isEmpty) {
            Validation.addError("Profile Photo", "No image found for Profile Photo")
          }
        }

        // landingPageImage validations and prepare to persist landingPageImage file
        val landingPageImageOption = landingPageImage match {
          case None => None
          case Some(imageFile) => {
            val parsedImage = ImageUtil.parseImage(imageFile)
            parsedImage.map(image => {
              val (width, height) = (image.getWidth, image.getHeight)
              Validation.isTrue("Landing Page Image must be at least " + Celebrity.minLandingPageImageWidth + " in width and " + Celebrity.minLandingPageImageHeight + " in height - resolution was " + width + "x" + height,
                width >= Celebrity.minLandingPageImageWidth && height >= Celebrity.minLandingPageImageHeight)
            })
            parsedImage
          }
        }

        // logo validations and prepare to persist logo file
        val logoImageImageOption = logoImage match {
          case None => None
          case Some(imageFile) => {
            val parsedImage = ImageUtil.parseImage(imageFile)
            parsedImage.map(image => {
              val (width, height) = (image.getWidth, image.getHeight)
              Validation.isTrue("Logo Image must be at least " + Celebrity.minLogoWidth + " in width and " + Celebrity.minLogoWidth + " in height - resolution was " + width + "x" + height,
                width >= Celebrity.minLogoWidth && height >= Celebrity.minLogoWidth)
              Validation.isTrue("Logo Image must be square. Resolution was " + width + "x" + height, width == height)
            })
            parsedImage
          }
        }

        // publishedStatusString validations
        val publishedStatus = PublishedStatus(publishedStatusString) match {
          case Some(providedStatus) => providedStatus
          case None =>
            Validation.addError("Error setting celebrity's published status, please contact support", "")
            PublishedStatus.Unpublished
        }

        if (!validationErrors.isEmpty) {
          redirectWithValidationErrors(
            celebrityId = celebrityId, celebrityEmail = celebrityEmail, celebrityPassword = celebrityPassword,
            publicName = publicName, publishedStatusString = publishedStatusString, bio = bio,
            casualName = casualName, organization = organization, roleDescription = roleDescription, twitterUsername = twitterUsername)

        } else {
          val savedCelebrity = celebrity.withPublishedStatus(publishedStatus).save()
          // Celebrity must have been previously saved before saving with assets that live in blobstore
          if (profileImage.isDefined) savedCelebrity.saveWithProfilePhoto(profileImage.get)

          if (isCreate) {

            passwordValidationOrAccount.right.get.copy(celebrityId = Some(savedCelebrity.id)).save()

            // Send the order email
            val email = new HtmlEmail()
            email.setFrom("noreply@egraphs.com", "Egraphs")
            email.addTo(celebrityEmail, publicName)
            email.setSubject("Egraphs Celebrity Account Created")
            email.setMsg(views.Application.email.html.celebrity_created_email(celebrity = savedCelebrity, email = celebrityEmail).toString().trim())
            transactionalMail.send(email)
          }
          savedCelebrity.saveWithImageAssets(landingPageImageOption, logoImageImageOption)

          new Redirect(GetCelebrityAdminEndpoint.url(celebrityId = savedCelebrity.id).url + "?action=preview")
        }
      }
  }

  private def celebrityWithValues(celebrity: Celebrity,
                                  publicName: String,
                                  bio: String,
                                  casualName: String,
                                  organization: String,
                                  roleDescription: String,
                                  twitterUsername: String): Celebrity = {
    celebrity.copy(publicName = publicName,
      bio = bio,
      casualName = Utils.toOption(casualName),
      organization = organization,
      roleDescription = Utils.toOption(roleDescription),
      twitterUsername = Utils.toOption(twitterUsername)
    )
  }

  private def redirectWithValidationErrors(celebrityId: Long,
                                           celebrityEmail: String,
                                           celebrityPassword: String,
                                           publicName: String,
                                           publishedStatusString: String,
                                           bio: String,
                                           casualName: String,
                                           organization: String,
                                           roleDescription: String,
                                           twitterUsername: String): Redirect = {
    flash.put("celebrityId", celebrityId)
    flash.put("celebrityEmail", celebrityEmail)
    flash.put("celebrityPassword", celebrityPassword)
    flash.put("publicName", publicName)
    flash.put("publishedStatusString", publishedStatusString)
    flash.put("bio", bio)
    flash.put("casualName", casualName)
    flash.put("organization", organization)
    flash.put("roleDescription", roleDescription)
    flash.put("twitterUsername", twitterUsername)
    if (celebrityId == 0) {
      WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityAdminEndpoint.url())
    } else {
      WebsiteControllers.redirectWithValidationErrors(GetCelebrityAdminEndpoint.url(celebrityId = celebrityId))
    }
  }
}
