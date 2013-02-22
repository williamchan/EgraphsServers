package controllers

import controllers.api._
import checkout.{CheckoutEndpoints, CheckoutResourceControllerFactory, CheckoutResourceEndpoints}
import play.api.mvc.Controller
import services.http.{POSTControllerMethod, ControllerMethod, POSTApiControllerMethod}
import services.db.DBSession
import akka.actor.ActorRef
import models.{EnrollmentBatchStore, EnrollmentBatchServices, OrderQueryFilters, OrderStore}
import services.blobs.Blobs
import services.http.filters.HttpFilters
import services.ConsumerApplication
import models.checkout.CheckoutAdapterServices

object ApiControllers extends Controller
  with GetIOSClientEndpoint
  with GetCelebrityApiEndpoint
  with GetCelebrityEnrollmentTemplateApiEndpoint
  with GetCelebrityMobileAppInfoEndpoint
  with GetCelebrityProductsApiEndpoint
  with GetCelebrityOrdersApiEndpoint
  with GetCustomerApiEndpoint
  with GetCustomerEgraphsApiEndpoint
  with GetTopEgraphsApiEndpoint
  with PostCelebrityOrderApiEndpoint
  with PostEgraphApiEndpoint
  with PostEnrollmentSampleApiEndpoint
  with PostVideoAssetApiEndpoint

  with CheckoutResourceEndpoints
  with CheckoutEndpoints
{
  import services.AppConfig.instance

  override protected val consumerApp = instance[ConsumerApplication]
  override protected val config = instance[services.config.ConfigFileProxy]
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
  override protected def httpFilters = instance[HttpFilters]
  override protected def checkoutControllers = instance[CheckoutResourceControllerFactory]
  override protected def checkoutAdapters = instance[CheckoutAdapterServices]
}
