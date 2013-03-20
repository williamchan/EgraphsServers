package services.request

import models.CelebrityRequest
import models.CelebrityStore
import models.frontend.email.CelebrityRequestEmailViewModel
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.db.DBSession
import services.db.TransactionSerializable
import services.email.CelebrityRequestEmail
import services.http.EgraphsSession
import services.http.EgraphsSession.Conversions._

object PostCelebrityRequestHelper {

  def completeRequestStar(requestedStar: String, customerId: Long, emailAddress: String) = {

    play.Logger.info("Request a Star: starName is " + requestedStar + ", customerId is " + customerId)

    // add row to celebrityRequests table
    CelebrityRequest(
      celebrityName = requestedStar,
      customerId = customerId).save()

    CelebrityRequestEmail(
      CelebrityRequestEmailViewModel(
        requesterEmail = emailAddress,
        requestedStar = requestedStar
      )
    ).send()
  }
}