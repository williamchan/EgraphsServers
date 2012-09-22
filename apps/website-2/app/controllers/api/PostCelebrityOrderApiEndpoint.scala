package controllers.api

import models.enums.OrderReviewStatus
import models.Celebrity
import models.Order
import play.api.mvc.Controller
import play.api.mvc.Result
import services.db.DBSession
import services.http.filters.RequireAuthenticatedAccount
import services.http.filters.RequireCelebrityId
import services.http.filters.RequireOrderIdOfCelebrity
import services.http.ControllerMethod
import sjson.json.Serializer

private[controllers] trait PostCelebrityOrderApiEndpoint { this: Controller =>
  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def requireAuthenticatedAccount: RequireAuthenticatedAccount
  protected def requireCelebrityId: RequireCelebrityId
  protected def requireOrderIdOfCelebrity: RequireOrderIdOfCelebrity

  /**
   * Updates an existing .
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints the json spec]] for more info
   * about the params.
   */
  def postCelebrityOrder(reviewStatus: Option[String] = None,
    rejectionReason: Option[String] = None) = {
    controllerMethod() {
      requireAuthenticatedAccount() { accountRequest =>
        val celebAction = requireCelebrityId.inAccount(accountRequest.account) { celebrityRequest =>
          val celebrity = celebrityRequest.celeb
          val orderAction = requireOrderIdOfCelebrity(celebrity.id) { orderRequest =>
            val order = orderRequest.order
            postCelebrityOrderResult(reviewStatus, rejectionReason, order, celebrity)
          }
          orderAction(celebrityRequest)
        }
        celebAction(accountRequest)
      }
    }
  }

  // this is everything that isn't context
  private def postCelebrityOrderResult(reviewStatus: Option[String], rejectionReason: Option[String], order: Order, celebrity: Celebrity): Result = {
    OrderReviewStatus.apply(reviewStatus.getOrElse("")) match {
      case Some(OrderReviewStatus.RejectedByCelebrity) => {
        val rejectedOrder = order.rejectByCelebrity(celebrity, rejectionReason = rejectionReason).save()
        Ok(Serializer.SJSON.toJSON(rejectedOrder.renderedForApi))
      }
      case _ => Ok(Serializer.SJSON.toJSON(order.renderedForApi))
    }
  }
}

