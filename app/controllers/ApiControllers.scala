package controllers

import api.{PostEnrollmentSampleApiEndpoint, GetCelebrityApiEndpoint, GetCelebrityOrdersApiEndpoint, PostEgraphApiEndpoint}
import play.mvc.Controller
import services.http.{OrderRequestFilters, CelebrityAccountRequestFilters, DBTransaction}
import services.Mail
import models.{OrderQueryFilters, EnrollmentBatchServices, OrderStore}

object ApiControllers extends Controller
  with GetCelebrityApiEndpoint
  with GetCelebrityOrdersApiEndpoint
  with PostEgraphApiEndpoint
  with PostEnrollmentSampleApiEndpoint
  with DBTransaction
{
  import services.AppConfig.instance

  override protected def orderStore = instance[OrderStore]
  override protected def orderQueryFilters = instance[OrderQueryFilters]
  override protected def enrollmentBatchServices = instance[EnrollmentBatchServices]
  override protected def celebFilters = instance[CelebrityAccountRequestFilters]
  override protected def orderFilters = instance[OrderRequestFilters]
  override protected def mail = instance[Mail]
}
