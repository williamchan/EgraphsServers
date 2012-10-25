package controllers

import website._
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.Redirect
import services.blobs.Blobs
import services.mail.{BulkMailList, TransactionalMail}
import services.payment.Payment
import models._
import models.filters._
import services.db.DBSession
import services.social.FacebookAppId
import services.http._
import services.http.filters._
import forms.{AccountRecoverFormFactory, AccountPasswordResetFormFactory, AccountSettingsFormFactory, CustomerLoginFormFactory}
import forms.purchase.{PurchaseFormChecksFactory, FormReaders, PurchaseFormFactory}
import services.mvc.celebrity.CatalogStarsQuery
import services.mvc.{OrderCompleteViewModelFactory, StorefrontBreadcrumbData}
import services.config.ConfigFileProxy
import services.db.Schema

object WebsiteControllers extends Controller with AllWebsiteEndpoints
{
  import services.AppConfig.instance
  import services.AppConfig.annotatedInstance

  // Provide endpoint dependencies
  override protected val config = instance[ConfigFileProxy]
  override protected val facebookAppId = annotatedInstance[FacebookAppId, String]

  override protected def breadcrumbData = instance[StorefrontBreadcrumbData]
  override protected def customerLoginForms = instance[CustomerLoginFormFactory]
  override protected def accountSettingsForms = instance[AccountSettingsFormFactory]
  override protected def accountPasswordResetForms = instance[AccountPasswordResetFormFactory]
  override protected def accountRecoverForms = instance[AccountRecoverFormFactory]

  override protected def catalogStarsQuery: CatalogStarsQuery = instance[CatalogStarsQuery]
  override protected def orderCompleteViewModelFactory: OrderCompleteViewModelFactory = instance[OrderCompleteViewModelFactory]

  override protected def checkPurchaseField: PurchaseFormChecksFactory = instance[PurchaseFormChecksFactory]

  override protected val purchaseFormFactory = instance[PurchaseFormFactory]
  override protected val formReaders = instance[FormReaders]
  override protected val dbSession = instance[DBSession]
  override protected val controllerMethod = instance[ControllerMethod]
  override protected val postController = instance[POSTControllerMethod]
  override protected val httpFilters = instance[HttpFilters]

  override protected val blobs = instance[Blobs]
  override protected val transactionalMail = instance[TransactionalMail]
  override protected val bulkMailList = instance[BulkMailList]
  override protected val payment = instance[Payment]
  override protected def schema = instance[Schema]

  override protected def enrollmentBatchStore = instance[EnrollmentBatchStore]
  override protected def inventoryBatchStore = instance[InventoryBatchStore]
  override protected def egraphStore = instance[EgraphStore]
  override protected val accountStore = instance[AccountStore]
  override protected val administratorStore = instance[AdministratorStore]
  override protected val celebrityStore = instance[CelebrityStore]
  override protected val customerStore = instance[CustomerStore]
  override protected def filterStore = instance[FilterStore]
  override protected val orderStore = instance[OrderStore]
  override protected val printOrderStore = instance[PrintOrderStore]
  override protected val productStore = instance[ProductStore]
  
  override protected val egraphQueryFilters = instance[EgraphQueryFilters]
  override protected val inventoryBatchQueryFilters = instance[InventoryBatchQueryFilters]
  override protected val orderQueryFilters = instance[OrderQueryFilters]
  override protected val printOrderQueryFilters = instance[PrintOrderQueryFilters]
}
