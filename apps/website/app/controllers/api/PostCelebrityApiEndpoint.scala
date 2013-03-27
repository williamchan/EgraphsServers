package controllers.api

import play.api.mvc._
import play.api.libs.json._
import services.http.filters.HttpFilters
import services.http.POSTApiControllerMethod
import models.JsCelebrityContactInfo
import models.JsCelebrityDepositInfo
import models.DecryptedCelebritySecureInfo
import services.logging.Logging

private[controllers] trait PostCelebrityApiEndpoint 
  extends Logging
{ this: Controller =>
  protected def postApiController: POSTApiControllerMethod
  protected def httpFilters: HttpFilters

  def postCelebrityContactInfo() = {
    postApiController() {
      httpFilters.requireAuthenticatedAccount.inRequest() { account =>
        httpFilters.requireCelebrityId.inAccount(account) { celebrity =>
          Action { request =>
            log(s"postCelebrityContactInfo attempt for celebrity $celebrity")
            val maybeContactInfo = request.body.asJson.flatMap(_.asOpt[JsCelebrityContactInfo])
            maybeContactInfo match {
              case None => 
                BadRequest("Could not parse json into JsCelebrityContactInfo.")
              case Some(contactInfo) =>
                val existingSecureInfo = celebrity.secureInfo.getOrElse(DecryptedCelebritySecureInfo())
                val secureInfo = existingSecureInfo.updateFromContactInfo(contactInfo).encrypt.save()
                celebrity.copy(twitterUsername = contactInfo.twitterUsername, secureInfoId = Some(secureInfo.id)).save()
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
            val maybeDepositInfo = request.body.asJson.flatMap(_.asOpt[JsCelebrityDepositInfo])
            maybeDepositInfo match {
              case None =>
                BadRequest("Could not parse json into JsCelebrityDepositInfo.")
              case Some(depositInfo) =>
                if (!depositInfo.isDepositAccountChange.isDefined) {
                  BadRequest("Must have value isDepositAccountChange.")
                } else {
                  val existingSecureInfo = celebrity.secureInfo.getOrElse(DecryptedCelebritySecureInfo())
                  val updatedSecureInfo = existingSecureInfo.updateFromDepositInfo(depositInfo)
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