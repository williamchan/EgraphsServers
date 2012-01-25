package controllers

import api.{PostEnrollmentSampleApiEndpoint, GetCelebrityApiEndpoint, GetCelebrityOrdersApiEndpoint, PostEgraphApiEndpoint}
import play.mvc.Controller
import models.OrderStore.FindByCelebrity.ActionableOnly
import models.{EnrollmentBatchServices, OrderStore}
import services.http.{OrderRequestFilters, CelebrityAccountRequestFilters, DBTransaction}

object ApiControllers extends Controller
  with GetCelebrityApiEndpoint
  with GetCelebrityOrdersApiEndpoint
  with PostEgraphApiEndpoint
  with PostEnrollmentSampleApiEndpoint
  with DBTransaction
{
  import services.AppConfig.instance

  protected def orderStore = instance[OrderStore]
  protected def actionableOrderFilter = instance[ActionableOnly]
  protected def enrollmentBatchServices = instance[EnrollmentBatchServices]
  protected def celebFilters = instance[CelebrityAccountRequestFilters]
  protected def orderFilters = instance[OrderRequestFilters]
}
