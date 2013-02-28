package controllers.api

import play.api.mvc._
import play.api.libs.json._
import services.http.filters.HttpFilters
import services.http.POSTApiControllerMethod
import models.JsCelebrityContactInfo
import models.JsCelebrityDepositInfo
import models.DecryptedCelebritySecureInfo

private[controllers] trait PostCelebrityApiEndpoint { this: Controller =>
  protected def postApiController: POSTApiControllerMethod
  protected def httpFilters: HttpFilters

  def postCelebrityContactInfo() = {
    postApiController() {
      httpFilters.requireAuthenticatedAccount.inRequest() { account =>
        httpFilters.requireCelebrityId.inAccount(account) { celebrity =>
          Action { request =>
            play.api.Logger.info(s"postCelebrityContactInfo attempt for celebrity $celebrity")
            val maybeContactInfo = request.body.asJson.map(_.asOpt[JsCelebrityContactInfo]).flatten
            maybeContactInfo match {
              case None => 
                BadRequest("Could not parse json into JsCelebrityContactInfo.")
              case Some(contactInfo) =>
                contactInfo.twitterUsername.map(twitter => celebrity.copy(twitterUsername = Some(twitter)).save())
                val existingSecureInfo = celebrity.secureInfo.getOrElse(DecryptedCelebritySecureInfo())
                val secureInfo = existingSecureInfo.copy(
                  contactEmail = contactInfo.contactEmail.map(_.value),
                  smsPhone = contactInfo.smsPhone,
                  voicePhone = contactInfo.voicePhone,
                  agentEmail = contactInfo.agentEmail.map(_.value)
                ).encrypt.save()
                celebrity.copy(secureInfoId = Some(secureInfo.id)).save()
                Ok
            }
          }
        }
      }
    }
  }

  def postCelebrityDepositInfo() = {
    postApiController() {
      httpFilters.requireAuthenticatedAccount.inRequest() { account =>
        httpFilters.requireCelebrityId.inAccount(account) { celebrity =>
          Action { request =>
            play.api.Logger.info(s"postCelebrityDepositInfo attempt for celebrity $celebrity")
            val maybeDepositInfo = request.body.asJson.map(_.asOpt[JsCelebrityDepositInfo]).flatten
            maybeDepositInfo match {
              case None =>
                BadRequest("Could not parse json into JsCelebrityDepositInfo.")
              case Some(depositInfo) =>
                if (!depositInfo.isDepositAccountChange.isDefined) {
                  BadRequest("Must have value isDepositAccountChange.")
                } else {
                  val existingSecureInfo = celebrity.secureInfo.getOrElse(DecryptedCelebritySecureInfo())
                  val partialSecureInfo = existingSecureInfo.copy(
                    streetAddress = depositInfo.streetAddress,
                    city = depositInfo.city,
                    postalCode = depositInfo.postalCode,
                    country = depositInfo.country)

                  val updatedSecureInfo = if (depositInfo.isDepositAccountChange.get) {
                    partialSecureInfo.withDepositAccountType(depositInfo.depositAccountType)
                      .withDepositAccountRoutingNumber(depositInfo.depositAccountRoutingNumber)
                      .withDepositAccountNumber(depositInfo.depositAccountNumber)
                  } else partialSecureInfo
                  val savedSecureInfo = updatedSecureInfo.encrypt.save()
                  celebrity.copy(secureInfoId = Some(savedSecureInfo.id)).save()
                  Ok
                }
            }
          }
        }
      }
    }
  }
}