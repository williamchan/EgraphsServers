package controllers.website.admin

import models._
import enums.PublishedStatus
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import java.io.File
import play.api.mvc.MultipartFormData
import java.awt.image.BufferedImage
import services.{ImageUtil, Utils}
import services.mail.TransactionalMail
import services.blobs.Blobs.Conversions._
import controllers.routes.WebsiteControllers.{getCreateCelebrityAdmin, getCelebrityAdmin}
import org.joda.time.DateTimeConstants
import egraphs.playutils.Gender
import services.http.forms.FormConstraints

trait PostCelebrityAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def transactionalMail: TransactionalMail
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def formConstraints: FormConstraints
  
  def postCreateCelebrityAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession(parser = parse.multipartFormData) { case (admin, adminAccount) =>
      Action(parse.multipartFormData) { implicit request =>
        
        val (profileImageFile, landingPageImageFile, logoImageFile, profileImageOption, landingPageImageOption, logoImageOption) = getUploadedImages(request.body)
      	
      	val form = Form(mapping(
              "celebrityEmail" -> email.verifying(nonEmpty, formConstraints.isUniqueEmail),
              "celebrityPassword" -> nonEmptyText.verifying(formConstraints.isPasswordValid),
              "publicName" -> nonEmptyText(maxLength = 128),
              "publishedStatusString" -> nonEmptyText.verifying(isCelebrityPublishedStatus),
              "bio" -> nonEmptyText,
              "casualName" -> text,
              "gender" -> nonEmptyText.verifying(isGender),
              "organization" -> nonEmptyText(maxLength = 128),
              "roleDescription" -> nonEmptyText(maxLength = 128),
              "twitterUsername" -> text
          )(PostCreateCelebrityForm.apply)(PostCreateCelebrityForm.unapply)
            .verifying(
              isUniqueUrlSlug(),
              profileImageIsValid(profileImageFile),
              landingPageImageIsValid(landingPageImageOption),
              logoImageIsValid(logoImageOption)
      		))
        
        form.bindFromRequest.fold(
            formWithErrors => {
              val data = formWithErrors.data
              val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
              val url = getCreateCelebrityAdmin.url
              Redirect(url).flashing(
                ("errors" -> errors.mkString(", ")), 
		        ("celebrityEmail" -> data.get("celebrityEmail").getOrElse("")), 
		        ("celebrityPassword" -> data.get("celebrityPassword").getOrElse("")), 
		        ("publicName" -> data.get("publicName").getOrElse("")), 
		        ("publishedStatusString" -> data.get("publishedStatusString").getOrElse("")), 
		        ("bio" -> data.get("casualName").getOrElse("")), 
		        ("casualName" -> data.get("casualName").getOrElse("")),
		        ("gender" -> data.get("gender").getOrElse("")),
		        ("organization" -> data.get("roleDescription").getOrElse("")), 
		        ("roleDescription" -> data.get("roleDescription").getOrElse("")), 
		        ("twitterUsername" -> data.get("twitterUsername").getOrElse(""))
              )
            },
            validForm => {
              // Save Celebrity
              val publishedStatus = PublishedStatus(validForm.publishedStatusString).get // must be valid
              val gender = Gender(validForm.gender).get // must be valid
              val savedCelebrity = Celebrity().copy(
                  publicName = validForm.publicName,
                  bio = validForm.bio,
                  casualName = Utils.toOption(validForm.casualName),
                  organization = validForm.organization,
                  roleDescription = validForm.roleDescription,
                  twitterUsername = Utils.toOption(removeAtSymbol(validForm.twitterUsername)))
                  .withPublishedStatus(publishedStatus)
                  .withGender(gender).save()
              
              // Save Celebrity image assets
              val savedWithImages = savedCelebrity.saveWithImageAssets(landingPageImageOption, logoImageOption)
              profileImageFile.map(f => savedWithImages.saveWithProfilePhoto(f))
              
              // Save Account
              val acct = accountStore.findByEmail(validForm.celebrityEmail).getOrElse(Account(email = validForm.celebrityEmail))
              val savedAccount = acct.copy(celebrityId = Some(savedWithImages.id)).withPassword(validForm.celebrityPassword).right.get.save()
              
              savedWithImages.sendWelcomeEmail(savedAccount.email)
              
              Redirect(getCelebrityAdmin(celebrityId = savedWithImages.id).url + "?action=preview")
            }
          )
      }
  	}
  }
  
  def postCelebrityAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession(parser = parse.multipartFormData) { case (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId, parser = parse.multipartFormData) { (celeb) =>
        Action(parse.multipartFormData) { implicit request =>
	      
          val (profileImageFile, landingPageImageFile, logoImageFile, profileImageOption, landingPageImageOption, logoImageOption) = getUploadedImages(request.body)
	      	
	      val form = Form(mapping(
	            "publicName" -> nonEmptyText(maxLength = 128),
	            "publishedStatusString" -> nonEmptyText.verifying(isCelebrityPublishedStatus),
	            "bio" -> nonEmptyText,
	            "casualName" -> text,
	            "gender" -> nonEmptyText.verifying(isGender),
	            "organization" -> nonEmptyText(maxLength = 128),
	            "roleDescription" -> nonEmptyText(maxLength = 128),
	            "twitterUsername" -> text
	        )(PostUpdateCelebrityForm.apply)(PostUpdateCelebrityForm.unapply)
	          .verifying(
	              isUniqueUrlSlug(Some(celebrityId)),
	              profileImageIsValid(profileImageFile),
	              landingPageImageIsValid(landingPageImageOption),
	              logoImageIsValid(logoImageOption)
	      	)
	      )
	        
	      form.bindFromRequest.fold(
	          formWithErrors => {
	            val data = formWithErrors.data
	            val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
	            val url = getCelebrityAdmin(celebrityId = celebrityId).url
	            Redirect(url).flashing(
	              ("errors" -> errors.mkString(", ")), 
			      ("celebrityEmail" -> data.get("celebrityEmail").getOrElse("")), 
			      ("celebrityPassword" -> data.get("celebrityPassword").getOrElse("")), 
			      ("publicName" -> data.get("publicName").getOrElse("")), 
			      ("publishedStatusString" -> data.get("publishedStatusString").getOrElse("")), 
			      ("bio" -> data.get("casualName").getOrElse("")), 
			      ("casualName" -> data.get("casualName").getOrElse("")),
			      ("gender" -> data.get("gender").getOrElse("")),
			      ("organization" -> data.get("roleDescription").getOrElse("")), 
			      ("roleDescription" -> data.get("roleDescription").getOrElse("")), 
			      ("twitterUsername" -> data.get("twitterUsername").getOrElse(""))
	            )
	          },
	          validForm => {
	            val publishedStatus = PublishedStatus(validForm.publishedStatusString).get // must be valid
	            val gender = Gender(validForm.gender).get // must be valid
	            val savedCelebrity = celeb.copy(
	                publicName = validForm.publicName,
	                bio = validForm.bio,
	                casualName = Utils.toOption(validForm.casualName),
	                organization = validForm.organization,
	                roleDescription = validForm.roleDescription,
	                twitterUsername = Utils.toOption(validForm.twitterUsername))
	                .withPublishedStatus(publishedStatus)
	                .withGender(gender).save()
	              
	            val savedWithImages = savedCelebrity.saveWithImageAssets(landingPageImageOption, logoImageOption)
	            profileImageFile.map(f => savedWithImages.saveWithProfilePhoto(f))
	              
	            Redirect(getCelebrityAdmin(celebrityId = savedWithImages.id).url + "?action=preview")
	          }
	        )
	      }
      }
  	}
  }

  def postExpectedOrderDelay(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() {
      case (admin, adminAccount) =>
        httpFilters.requireCelebrityId(celebrityId) { celebrity =>
          Action { implicit request =>
            val expectedOrderDelayInDaysForm = Form(
              single(
                "delayInDays" -> number))
              .bindFromRequest()

            expectedOrderDelayInDaysForm.fold(
              e => BadRequest("No order delay found in request.")
              ,
              expectedOrderDelayInDays => {
                celebrity.copy(expectedOrderDelayInMinutes = expectedOrderDelayInDays * DateTimeConstants.MINUTES_PER_DAY).save()
                Ok(expectedOrderDelayInDays.toString)
              }
            )
          }
        }
    }
  }

  def postOfficialTwitterScreenName(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() {
      case (admin, adminAccount) =>
        httpFilters.requireCelebrityId(celebrityId) { celebrity =>
          Action { implicit request =>
            val officialTwitterScreenNameForm = Form(
              single(
                "officialTwitterScreenName" -> text))
              .bindFromRequest()

            officialTwitterScreenNameForm.fold(
              e => BadRequest("No twitter screen name in request."),
              officialScreenName => {
                val screenName = removeAtSymbol(officialScreenName)
                celebrity.copy(twitterUsername = Some(screenName)).save()
                Ok(screenName)
              })
          }
        }
    }
  }

  private def removeAtSymbol(screenName: String): String = {
    // take off the '@' if it is on there
    val trimmed = screenName.trim
    if (!trimmed.isEmpty && trimmed.charAt(0) == '@') {
      trimmed.tail
    } else {
      trimmed
    }
  }

  private trait PostCelebrityForm {
     val publicName: String
     val publishedStatusString: String
     val bio: String
     val casualName: String
     val gender: String
     val organization: String
     val roleDescription: String
     val twitterUsername: String
  }
  
  private case class PostUpdateCelebrityForm(
     publicName: String,
     publishedStatusString: String,
     bio: String,
     casualName: String,
     gender: String,
     organization: String,
     roleDescription: String,
     twitterUsername: String
  ) extends PostCelebrityForm
  
  private case class PostCreateCelebrityForm(
     celebrityEmail: String,
     celebrityPassword: String,
     publicName: String,
     publishedStatusString: String,
     bio: String,
     casualName: String,
     gender: String,
     organization: String,
     roleDescription: String,
     twitterUsername: String
  ) extends PostCelebrityForm
  
  private def getUploadedImages(body: MultipartFormData[play.api.libs.Files.TemporaryFile])
  : (Option[File], Option[File], Option[File], Option[BufferedImage], Option[BufferedImage], Option[BufferedImage]) = 
  {
    val profileImageFile = body.file("profileImage").map(_.ref.file)
    val landingPageImageFile = body.file("landingPageImage").map(_.ref.file)
    val logoImageFile = body.file("logoImage").map(_.ref.file)
    val profileImageOption = if (profileImageFile.isDefined) ImageUtil.parseImage(profileImageFile.get) else None
    val landingPageImageOption = if (landingPageImageFile.isDefined) ImageUtil.parseImage(landingPageImageFile.get) else None
    val logoImageOption = if (logoImageFile.isDefined) ImageUtil.parseImage(logoImageFile.get) else None
    (profileImageFile, landingPageImageFile, logoImageFile, profileImageOption, landingPageImageOption, logoImageOption)
  }

  private def isUniqueUrlSlug(celebrityId: Option[Long] = None): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      val isCreate = celebrityId.isEmpty
      
      val celebrityUrlSlug = Celebrity(publicName = form.publicName).urlSlug
      val celebrityByUrlSlug = celebrityStore.findByUrlSlug(celebrityUrlSlug)
      val isUniqueUrlSlug = if (isCreate) {
        celebrityByUrlSlug.isEmpty
      } else {
        celebrityByUrlSlug.isEmpty || (celebrityByUrlSlug.isDefined && celebrityId == Some(celebrityByUrlSlug.get.id))
      }
      if (isUniqueUrlSlug) Valid else Invalid("Celebrity with same website name exists. Provide different public name")
    }
  }

  private def profileImageIsValid(profileImageFile: Option[File]): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      profileImageFile.map{ f =>
        if (ImageUtil.getDimensions(f).isEmpty) Invalid("No image found for Profile Photo") else Valid
      }.getOrElse(Valid)
    }
  }
  
  private def landingPageImageIsValid(landingPageImageOption: Option[BufferedImage]): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      landingPageImageOption.map { landingPageImage =>
        val (width, height) = (landingPageImage.getWidth, landingPageImage.getHeight)
        if (width >= LandingPageImage.minImageWidth && height >= LandingPageImage.minImageHeight) {
          Valid
        } else {
          Invalid("Landing Page Image must be at least " + LandingPageImage.minImageWidth + " in width and " + LandingPageImage.minImageHeight + " in height - resolution was " + width + "x" + height)
        }
      }.getOrElse(Valid)
    }
  }
  
  private def logoImageIsValid(logoImageOption: Option[BufferedImage]): Constraint[PostCelebrityForm] = {
    Constraint { form: PostCelebrityForm =>
      logoImageOption.map { logoImage =>
        val (width, height) = (logoImage.getWidth, logoImage.getHeight)
        val isMinWidth = (width >= Celebrity.minLogoWidth && height >= Celebrity.minLogoWidth)
        val isSquare = (width == height)
        if (isMinWidth && isSquare) {
          Valid
        } else {
          Invalid("Logo Image must be square and at least " + Celebrity.minLogoWidth + " on a side. Resolution was " + width + "x" + height)
        }
      }.getOrElse(Valid)
    }
  }

  private def isCelebrityPublishedStatus: Constraint[String] = {
    Constraint { s: String =>
      PublishedStatus(s) match {
        case Some(providedStatus) => Valid
        case None => Invalid("Error setting product's published status, please contact support")
      }
    }
  }

  private def isGender: Constraint[String] = {
    Constraint { s: String =>
      Gender(s) match {
        case Some(providedStatus) => Valid
        case None => Invalid("Error setting celebrity's gender, please contact support")
      }
    }
  }
}
