package controllers

import api._
import play.mvc.Controller
import models.{OrderQueryFilters, EnrollmentBatchServices, OrderStore}
import services.http.{ControllerMethod, OrderRequestFilters, CelebrityAccountRequestFilters}
import services.db.DBSession
import akka.actor.ActorRef

object ApiControllers extends Controller
  with GetCelebrityEnrollmentTemplateApiEndpoint
  with GetCelebrityApiEndpoint
  with GetCelebrityProductsApiEndpoint
  with GetCelebrityOrdersApiEndpoint
  with PostEgraphApiEndpoint
  with PostEnrollmentSampleApiEndpoint
{
  import services.AppConfig.instance


  override protected def egraphActor: ActorRef = actors.EgraphActor.actor
  override protected def dbSession: DBSession = instance[DBSession]
  override protected def controllerMethod = instance[ControllerMethod]
  override protected def orderStore = instance[OrderStore]
  override protected def orderQueryFilters = instance[OrderQueryFilters]
  override protected def enrollmentBatchServices = instance[EnrollmentBatchServices]
  override protected def celebFilters = instance[CelebrityAccountRequestFilters]
  override protected def orderFilters = instance[OrderRequestFilters]
}
