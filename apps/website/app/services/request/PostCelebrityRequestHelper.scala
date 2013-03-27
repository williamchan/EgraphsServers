package services.request

import models.{CelebrityRequest, CelebrityRequestStore}
import models.frontend.email.CelebrityRequestEmailViewModel
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.AppConfig
import services.db.DBSession
import services.db.TransactionSerializable
import services.email.CelebrityRequestEmail
import services.http.EgraphsSession
import services.http.EgraphsSession.Conversions._

object PostCelebrityRequestHelper {

  def completeRequestStar(requestedStar: String, customerId: Long, emailAddress: String) = {

    play.Logger.info("Request a Star: starName is " + requestedStar + ", customerId is " + customerId)

    // Only add a row if there's NOT already a request for given star by given customer
    val celebrityStore = AppConfig.instance[CelebrityRequestStore]
    val maybeCelebrityRequest = celebrityStore.getCelebrityRequestByCustomerIdAndCelebrityName(customerId, requestedStar)

    if (!maybeCelebrityRequest.isDefined) {
      // Add row to celebrityRequests table
      CelebrityRequest(
        celebrityName = requestedStar,
        customerId = customerId
      ).save()
    }

    CelebrityRequestEmail(
      CelebrityRequestEmailViewModel(
        requesterEmail = emailAddress,
        requestedStar = requestedStar
      )
    ).send()
  }
}