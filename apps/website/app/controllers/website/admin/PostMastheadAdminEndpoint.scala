package controllers.website.admin

import play.api.mvc.{Request, Action, Controller}
import services.http.POSTControllerMethod
import models.{Masthead, LandingPageImage, Celebrity, MastheadStore}
import services.http.filters.HttpFilters
import services.{Utils, ImageUtil}
import java.awt.image.BufferedImage
import play.api.data.validation.{Invalid, Valid, Constraint}
import models.enums.{CallToActionType, PublishedStatus}
import play.api.data._
import play.api.data.Forms._


trait PostMastheadAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def mastheadStore: MastheadStore


  def postMastheadAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession(parser = parse.multipartFormData) { case (admin, adminAccount) =>
      Action(parse.multipartFormData) { implicit request =>
        val landingPageImageFile = request.body.file("landingPageImage").map(_.ref.file).filterNot(_.length == 0)
        val landingPageImageOption = if (landingPageImageFile.isDefined) ImageUtil.parseImage(landingPageImageFile.get) else None

        val form = Form(mapping(
          "name" -> text,
          "mastheadId" -> longNumber,
          "headline" -> nonEmptyText,
          "subtitle" -> text,
          "publishedStatusString" -> nonEmptyText.verifying(isMastheadPublishedStatus),
          "callToActionTypeString" -> nonEmptyText.verifying(isCallToActionType),
          "callToActionTarget" -> text,
          "callToActionText" -> text
        )(PostMastheadForm.apply)(PostMastheadForm.unapply).verifying(
          landingPageImageIsValid(landingPageImageOption)
        ))

        form.bindFromRequest.fold(
          formWithErrors => {
            val data = formWithErrors.data
            val errors = for  (error <- formWithErrors.errors) yield {error.key + ": " + error.message }
            val url = Form("mastheadId" -> longNumber).bindFromRequest.fold(
              formWithErrors => controllers.routes.WebsiteControllers.getCreateMastheadAdmin().url, validForm =>
              if (validForm > 0) {
                controllers.routes.WebsiteControllers.getMastheadAdmin(validForm).url
              } else {
                controllers.routes.WebsiteControllers.getCreateMastheadAdmin().url
              }
            )

            Redirect(url).flashing(
              ("errors" -> errors.mkString(", ")),
              ("name" -> data.get("name").getOrElse("")),
              ("headline" -> data.get("headline").getOrElse("")),
              ("subtitle" -> data.get("subtitle").getOrElse("")),
              ("publishedStatusString" -> data.get("publishedStatusString").getOrElse("")),
              ("callToActionTypeString" -> data.get("callToActionTypeStrine").getOrElse("")),
              ("callToActionTarget" -> data.get("callToActionTarget").getOrElse("")),
              ("callToActionText" -> data.get("callToActionText").getOrElse(""))
            )
          },
          validForm => {
            val publishedStatus = PublishedStatus(validForm.publishedStatusString).get
            val callToActionType = CallToActionType(validForm.callToActionTypeString).get
            val mastheadId = validForm.mastheadId
            val mastheadOption: Option[Masthead] = mastheadId match {
              case 0 => Option(Masthead())
              case _ => mastheadStore.findById(mastheadId)
            }

            mastheadOption match {
              case Some(masthead) => {
                // Save with new data. Assigns a unique id to masthead, so that the optional
                // saveWithLandingPageImage() doesn't use an ID of 0 for the blobstore.
                val savedMasthead = masthead.copy(
                  name = validForm.name,
                  headline = validForm.headline,
                  subtitle = Utils.toOption(validForm.subtitle),
                  callToActionTarget = validForm.callToActionTarget,
                  callToActionText = validForm.callToActionText
                ).withPublishedStatus(publishedStatus).withCallToActionType(callToActionType).save()

                landingPageImageOption.map { _ =>
                  savedMasthead.saveWithLandingPageImage(landingPageImageOption)
                }

                Redirect(controllers.routes.WebsiteControllers.getMastheadAdmin(savedMasthead.id))
              }
              case None => NotFound("No masthead with id " + mastheadId + " exists!")
            }
          }
        )
      }
    }
  }

  private case class PostMastheadForm(
    name: String,
    mastheadId: Long,
    headline: String,
    subtitle: String,
    publishedStatusString: String,
    callToActionTypeString: String,
    callToActionTarget: String,
    callToActionText: String
  )

  private def landingPageImageIsValid(landingPageImageOption: Option[BufferedImage]): Constraint[PostMastheadForm] = {
    Constraint { form: PostMastheadForm =>
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

  private def isMastheadPublishedStatus: Constraint[String] = {
    Constraint { s: String =>
      PublishedStatus(s) match {
        case Some(providedStatus) => Valid
        case None => Invalid("Error setting masthead's published status, please contact support")
      }
    }
  }

  private def isCallToActionType: Constraint[String] = {
    Constraint { t: String =>
      CallToActionType(t) match {
        case Some(providedType) => Valid
        case None => Invalid("Error setting masthead's CTA type, please contact support")
      }
    }
  }

}
