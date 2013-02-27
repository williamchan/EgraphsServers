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

  protected def completeRequestStar(requestedStar: String, customerId: Long)(implicit request: Request[AnyContent]): Result = {

    val maybeCelebrity = {
      dbSession.connected(TransactionSerializable) {
        celebrityStore.findByPublicName(requestedStar)
      }
    }

    maybeCelebrity match {
      case None => {
        play.Logger.info(requestedStar + " is not currently on Egraphs. Wah wah.")

        // add row to celebrityRequests table
        dbSession.connected(TransactionSerializable) {
          CelebrityRequest(
            celebrityName = requestedStar,
            customerId = customerId).save()
        }
      }
      case Some(celebrity) => play.Logger.info("We already have that celebrity, silly! Buy an egraph from " + requestedStar + "!")
    }

    play.Logger.info("starName is " + requestedStar + ", customerId is " + customerId)
    Redirect(controllers.routes.WebsiteControllers.getMarketplaceResultPage(vertical = "")).withSession(
      request.session.withCustomerId(customerId).removeRequestedStar)
  }
}