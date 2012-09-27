package controllers

import api._
import play.mvc.Controller
import services.http._
import services.db.DBSession
import akka.actor.ActorRef
import models.{EnrollmentBatchStore, OrderQueryFilters, OrderStore}
import services.blobs.Blobs
import services.AppConfig._
import java.util.Properties
import models.EnrollmentBatchServices

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
  override protected def postApiController = instance[POSTApiControllerMethod]
  override protected def blobs = instance[Blobs]
  override protected def enrollmentBatchStore = instance[EnrollmentBatchStore]
  override protected def orderStore = instance[OrderStore]
  override protected def orderQueryFilters = instance[OrderQueryFilters]
  override protected def enrollmentBatchServices = instance[EnrollmentBatchServices]
  override protected def celebFilters = instance[CelebrityAccountRequestFilters]
  override protected def orderFilters = instance[OrderRequestFilters]
}
