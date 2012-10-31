package controllers.website.admin

import models._
import enums.PublishedStatus
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import services.http.SafePlayParams.Conversions._
import services.logging.Logging
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import java.io.File
import services.logging.Logging
import java.text.SimpleDateFormat
import services.http.SafePlayParams.Conversions._
import play.api.mvc.MultipartFormData
import java.awt.image.BufferedImage
import services.{Dimensions, ImageUtil, Utils}
import services.mail.TransactionalMail
import services.blobs.Blobs.Conversions._
import org.apache.commons.mail.HtmlEmail


// TODO(wchan): This code has become frankenstein. I'm so sorry.
trait PostCelebrityAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def transactionalMail: TransactionalMail
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  
  case class PostCelebrityForm(
     celebrityEmail: String,
     celebrityPassword: String,
     publicName: String,
     publishedStatusString: String,
     bio: String,
     casualName: String,
     organization: String,
     roleDescription: String,
     twitterUsername: String
  )

  def postCelebrityAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession(parser = parse.multipartFormData) { (admin, adminAccount) =>
      Action(parse.multipartFormData) { implicit request =>
        
        val celebrityId = Form("celebrityId" -> longNumber).bindFromRequest.fold(formWithErrors => 0L, validForm => validForm)
        val profileImageFile = request.body.file("profileImage").map(_.ref.file)
      	val landingPageImageFile = request.body.file("landingPageImage").map(_.ref.file)
      	val logoImageFile = request.body.file("logoImage").map(_.ref.file)
      	val profileImageOption = if (profileImageFile.isDefined) ImageUtil.parseImage(profileImageFile.get) else None
      	val landingPageImageOption = if (landingPageImageFile.isDefined) ImageUtil.parseImage(landingPageImageFile.get) else None
      	val logoImageOption = if (logoImageFile.isDefined) ImageUtil.parseImage(logoImageFile.get) else None
      	val isCreate = (celebrityId == 0)
      	
      	val form = Form(mapping(
              "celebrityEmail" -> email.verifying(nonEmpty),
              "celebrityPassword" -> text,
              "publicName" -> nonEmptyText(maxLength = 128),
              "publishedStatusString" -> nonEmptyText.verifying(isCelebrityPublishedStatus),
              "bio" -> nonEmptyText,
              "casualName" -> text,
              "organization" -> nonEmptyText(maxLength = 128),
              "roleDescription" -> nonEmptyText(maxLength = 128),
              "twitterUsername" -> text
          )(PostCelebrityForm.apply)(PostCelebrityForm.unapply)
            .verifying(
                isUniqueEmail(isCreate, celebrityId),
                isPasswordValid(isCreate, celebrityId),
                isUniqueUrlSlug(isCreate, celebrityId),
                profileImageIsValid(profileImageFile),
                landingPageImageIsValid(landingPageImageOption),
                logoImageIsValid(logoImageOption)
      		)
          )
        
        form.bindFromRequest.fold(
            formWithErrors => {
              val data = formWithErrors.data
              val errors = for (error <- formWithErrors.errors) yield {
                error.key + ": " + error.message
              }
              val url = if (isCreate) GetCreateCelebrityAdminEndpoint.url() else GetCelebrityAdminEndpoint.url(celebrityId = celebrityId)
              Redirect(url).flashing(
                ("errors" -> errors.mkString(", ")), 
		        ("celebrityId" -> celebrityId.toString), 
		        ("celebrityEmail" -> data.get("celebrityEmail").getOrElse("")), 
		        ("celebrityPassword" -> data.get("celebrityPassword").getOrElse("")), 
		        ("publicName" -> data.get("publicName").getOrElse("")), 
		        ("publishedStatusString" -> data.get("publishedStatusString").getOrElse("")), 
		        ("bio" -> data.get("casualName").getOrElse("")), 
		        ("casualName" -> data.get("casualName").getOrElse("")), 
		        ("organization" -> data.get("roleDescription").getOrElse("")), 
		        ("roleDescription" -> data.get("roleDescription").getOrElse("")), 
		        ("twitterUsername" -> data.get("twitterUsername").getOrElse(""))
              )
            },
            validForm => {
              val publishedStatus = PublishedStatus(validForm.publishedStatusString).getOrElse(PublishedStatus.Unpublished)
              val tmp = if (isCreate) Celebrity() else celebrityStore.get(celebrityId)
              val savedCelebrity = tmp.copy(
                  publicName = validForm.publicName,
                  bio = validForm.bio,
                  casualName = Utils.toOption(validForm.casualName),
                  organization = validForm.organization,
                  roleDescription = validForm.roleDescription,
                  twitterUsername = Utils.toOption(validForm.twitterUsername))
                  .withPublishedStatus(publishedStatus).save()
              
              // Celebrity must have been previously saved before saving with assets that live in blobstore
              profileImageFile.map(f => savedCelebrity.saveWithProfilePhoto(f))
              savedCelebrity.saveWithImageAssets(landingPageImageOption, logoImageOption)
              
              if (isCreate) {
                new Account(celebrityId = Some(savedCelebrity.id), email = validForm.celebrityEmail).withPassword(validForm.celebrityPassword).right.get.save
	            savedCelebrity.sendWelcomeEmail(savedCelebrity.account.email)
	          }
              
              Redirect(GetCelebrityAdminEndpoint.url(celebrityId = savedCelebrity.id) + "?action=preview")
            }
          )
      }
  	}
  }
  
  private def isUniqueEmail(isCreate: Boolean, celebrityId: Long): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      if (isCreate) {
        val preexistingAccount: Option[Account] = if (isCreate) accountStore.findByEmail(form.celebrityEmail) else accountStore.findByCelebrityId(celebrityId)
        if (preexistingAccount.isDefined) {
          val isUniqueEmail = preexistingAccount.get.celebrityId.isEmpty
          if (isUniqueEmail) Valid else Invalid("Celebrity with e-mail address already exists")
        } else {
          Valid
        }
      } else {
        Valid
      }
    }
  }
  
  private def isPasswordValid(isCreate: Boolean, celebrityId: Long): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      if (isCreate) {
        val preexistingAccount: Option[Account] = if (isCreate) accountStore.findByEmail(form.celebrityEmail) else accountStore.findByCelebrityId(celebrityId)
        val passwordValidationOrAccount: Either[Password.PasswordError, Account] = if (preexistingAccount.isDefined) {
          Right(preexistingAccount.get)
        } else {
          Account(email = form.celebrityEmail).withPassword(form.celebrityPassword)
        }
        if (passwordValidationOrAccount.isRight) Valid else Invalid("Password is invalid")
      } else {
        Valid
      }
    }
  }
  
  private def isUniqueUrlSlug(isCreate: Boolean, celebrityId: Long): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      val celebrityUrlSlug = Celebrity(publicName = form.publicName).urlSlug
      val celebrityByUrlSlug = celebrityStore.findByUrlSlug(celebrityUrlSlug)
      val isUniqueUrlSlug = if (isCreate) {
        celebrityByUrlSlug.isEmpty
      } else {
        celebrityByUrlSlug.isEmpty || (celebrityByUrlSlug.isDefined && celebrityByUrlSlug.get.id == celebrityId)
      }
      if (isUniqueUrlSlug) Valid else Invalid("Celebrity with same website name exists. Provide different public name")
    }
  }

  private def profileImageIsValid(profileImageFile: Option[File]): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      if (profileImageFile.isDefined) {
        val dimensions = ImageUtil.getDimensions(profileImageFile.get)
        if (dimensions.isEmpty) {
          Invalid("No image found for Profile Photo")
        } else {
          Valid
        }
      } else {
        Valid
      }
    }
  }
  
  private def landingPageImageIsValid(landingPageImageOption: Option[BufferedImage]): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      if (landingPageImageOption.isDefined) {
        val landingPageImage = landingPageImageOption.get
        val (width, height) = (landingPageImage.getWidth, landingPageImage.getHeight)
        if (width >= Celebrity.minLandingPageImageWidth && height >= Celebrity.minLandingPageImageHeight) {
          Valid
        } else {
          Invalid("Landing Page Image must be at least " + Celebrity.minLandingPageImageWidth + " in width and " + Celebrity.minLandingPageImageHeight + " in height - resolution was " + width + "x" + height)
        }
      } else {
        Valid
      }
    }
  }
  
  private def logoImageIsValid(logoImageOption: Option[BufferedImage]): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      if (logoImageOption.isDefined) {
        val logoImage = logoImageOption.get
        val (width, height) = (logoImage.getWidth, logoImage.getHeight)
        val isMinWidth = (width >= Celebrity.minLogoWidth && height >= Celebrity.minLogoWidth)
        val isSquare = (width == height)
        if (isMinWidth && isSquare) {
          Valid
        } else {
          Invalid("Logo Image must be square and at least " + Celebrity.minLogoWidth + " on a side. Resolution was " + width + "x" + height)
        }
      } else {
        Valid
      }
    }
  }

  // TODO: redundant with isProductPublishedStatus
  def isCelebrityPublishedStatus: Constraint[String] = {
    Constraint { s: String =>
      PublishedStatus(s) match {
        case Some(providedStatus) => Valid
        case None => Invalid("Error setting product's published status, please contact support")
      }
    }
  }
}
