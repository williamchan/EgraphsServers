package controllers

import controllers.api._
import play.api.mvc.Controller
import services.http.{PlayConfig, ControllerMethod}
import services.db.DBSession
import akka.actor.ActorRef
import models.{EnrollmentBatchStore, OrderQueryFilters, EnrollmentBatchServices, OrderStore}
import services.blobs.Blobs
import services.AppConfig._
import java.util.Properties
import services.http.filters.HttpFilters

object ApiControllers extends Controller
  with GetCelebrityApiEndpoint
  with GetCelebrityEnrollmentTemplateApiEndpoint
  with GetCelebrityMobileAppInfoEndpoint
  with GetCelebrityProductsApiEndpoint
  with GetCelebrityOrdersApiEndpoint
  with PostCelebrityOrderApiEndpoint
  with PostEgraphApiEndpoint
  with PostEnrollmentSampleApiEndpoint
{
  import services.AppConfig.instance

  override protected val playConfig = annotatedInstance[PlayConfig, Properties]
  override protected def egraphActor: ActorRef = actors.EgraphActor.actor
  override protected def enrollmentBatchActor: ActorRef = actors.EnrollmentBatchActor.actor
  override protected def dbSession: DBSession = instance[DBSession]
  override protected def controllerMethod = instance[ControllerMethod]
  override protected def blobs = instance[Blobs]
  override protected def enrollmentBatchStore = instance[EnrollmentBatchStore]
  override protected def orderStore = instance[OrderStore]
  override protected def orderQueryFilters = instance[OrderQueryFilters]
  override protected def enrollmentBatchServices = instance[EnrollmentBatchServices]
  override protected def httpFilters = instance[HttpFilters]  
}
