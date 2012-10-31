package controllers.api

import models.Celebrity
import models.Order
import models.enums.OrderReviewStatus
import play.api.mvc.Controller
import play.api.mvc.Result
import play.api.data._
import Forms._
import play.api.data.format.Formats._
import services.db.DBSession
import services.http.{WithoutDBConnection, POSTApiControllerMethod}
import services.http.filters.HttpFilters
import sjson.json.Serializer
import play.api.mvc.Action

private[controllers] trait PostCelebrityOrderApiEndpoint { this: Controller =>
  protected def dbSession: DBSession
  protected def postApiController: POSTApiControllerMethod
  protected def httpFilters: HttpFilters

  /**
   * Updates an existing Celebrity.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints the json spec]] for more info
   * about the params.
   */
  def postCelebrityOrder(orderId: Long) = postApiController() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCelebrityId.inAccount(account) { celebrity =>
        httpFilters.requireOrderIdOfCelebrity(orderId, celebrity.id) { order =>
          Action { implicit request =>
            val form = Form(tuple("reviewStatus" -> optional(text), "rejectionReason" -> optional(text)))

            form.bindFromRequest.fold(
              errors => BadRequest,
              reviewStatusAndRejectionReason => {
                val (reviewStatus, rejectionReason) = reviewStatusAndRejectionReason
                postCelebrityOrderResult(reviewStatus, rejectionReason, order, celebrity)
              }
            )
          }
        }
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

