package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import services.http.{ControllerMethod, OrderRequestFilters, CelebrityAccountRequestFilters}
import services.db.DBSession
import models.Order

private[controllers] trait PostCelebrityOrderApiEndpoint { this: Controller =>
  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def orderFilters: OrderRequestFilters

  /**
   * Updates an existing .
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints the json spec]] for more info
   * about the params.
   */
  def postCelebrityOrder(reviewStatus: Option[String] = None,
                         rejectionReason: Option[String] = None) = {
    controllerMethod() {
      celebFilters.requireCelebrityAccount {
        (account, celebrity) =>
          orderFilters.requireOrderIdOfCelebrity(celebrity.id) {
            order =>
              Order.ReviewStatus.all.get(reviewStatus.getOrElse("")) match {
                case Some(Order.ReviewStatus.RejectedByCelebrity) => {
                  val rejectedOrder = order.rejectByCelebrity(celebrity, rejectionReason = rejectionReason).save()
                  Serializer.SJSON.toJSON(rejectedOrder.renderedForApi)
                }
                case _ => Serializer.SJSON.toJSON(order.renderedForApi)
              }
          }
      }
    }
  }
}

