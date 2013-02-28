package services.request

import models.CelebrityRequest
import models.CelebrityStore
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.db.DBSession
import services.db.TransactionSerializable
import services.http.EgraphsSession
import services.http.EgraphsSession.Conversions._

trait PostCelebrityRequestHelper {
  protected def celebrityStore: CelebrityStore
  protected def dbSession: DBSession  

  protected def completeRequestStar(requestedStar: String, customerId: Long) = {

    // add row to celebrityRequests table
    dbSession.connected(TransactionSerializable) {
      CelebrityRequest(
        celebrityName = requestedStar,
        customerId = customerId).save()
    }

    play.Logger.info("Request a Star: starName is " + requestedStar + ", customerId is " + customerId)
  }
}