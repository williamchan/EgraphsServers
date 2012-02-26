package controllers

import api._
import play.mvc.Controller
import services.http.{OrderRequestFilters, CelebrityAccountRequestFilters, DBTransaction}
import models.{OrderQueryFilters, EnrollmentBatchServices, OrderStore}

object ApiControllers extends Controller
  with GetCelebrityEnrollmentTemplateApiEndpoint
  with GetCelebrityApiEndpoint
  with GetCelebrityProductsApiEndpoint
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
}
