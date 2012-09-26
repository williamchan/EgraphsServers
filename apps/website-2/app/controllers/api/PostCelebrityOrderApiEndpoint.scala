package controllers.api

import models.Celebrity
import models.Order
import models.enums.OrderReviewStatus
import play.api.mvc.Controller
import play.api.mvc.Result
import services.db.DBSession
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import sjson.json.Serializer

private[controllers] trait PostCelebrityOrderApiEndpoint { this: Controller =>
  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  /**
   * Updates an existing Celebrity.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints the json spec]] for more info
   * about the params.
   */
  def postCelebrityOrder(
    reviewStatus: Option[String] = None, 
    rejectionReason: Option[String] = None) = 
  {
    controllerMethod() {
      httpFilters.requireAuthenticatedAccount() { account =>
        httpFilters.requireCelebrityId.inAccount(account) { celebrity =>          
          httpFilters.requireOrderIdOfCelebrity(celebrity.id) { order =>            
            postCelebrityOrderResult(reviewStatus, rejectionReason, order, celebrity)
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

