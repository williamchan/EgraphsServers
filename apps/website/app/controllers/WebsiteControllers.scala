package controllers

import models._
import checkout.{CheckoutServices, LineItemStore, CheckoutAdapterServices}
import models.categories._
import website._
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.Redirect
import services.blobs.Blobs
import services.mail.{BulkMailList, TransactionalMail}
import services.payment.Payment
import services.ConsumerApplication
import services.blobs.Blobs
import services.db.DBSession
import services.social.FacebookAppId
import services.http._
import services.http.filters._
import forms.{AccountPasswordResetFormFactory, AccountSettingsFormFactory, FormConstraints}
import forms.purchase.{PurchaseFormChecksFactory, FormReaders, PurchaseFormFactory}
import services.mvc.celebrity.CatalogStarsQuery
import services.mvc.marketplace._
import services.mvc.{OrderCompleteViewModelFactory, StorefrontBreadcrumbData}
import services.config.ConfigFileProxy
import services.db.Schema
import services.mvc.landing.LandingMastheadsQuery
import services.cache.CacheFactory

object WebsiteControllers extends Controller with AllWebsiteEndpoints
{
  import services.AppConfig.instance
  import services.AppConfig.annotatedInstance

  // Provide endpoint dependencies
  override protected val config = instance[ConfigFileProxy]
  override protected val consumerApp = instance[ConsumerApplication]
  override protected val facebookAppId = annotatedInstance[FacebookAppId, String]

  override protected val breadcrumbData = instance[StorefrontBreadcrumbData]
  override protected val accountSettingsForms = instance[AccountSettingsFormFactory]
  override protected val accountPasswordResetForms = instance[AccountPasswordResetFormFactory]

  override protected val catalogStarsQuery: CatalogStarsQuery = instance[CatalogStarsQuery]
  override protected val landingMastheadsQuery: LandingMastheadsQuery = instance[LandingMastheadsQuery]
  override protected val orderCompleteViewModelFactory: OrderCompleteViewModelFactory = instance[OrderCompleteViewModelFactory]

  override protected val checkPurchaseField: PurchaseFormChecksFactory = instance[PurchaseFormChecksFactory]

  override protected val purchaseFormFactory = instance[PurchaseFormFactory]
  override protected val formReaders = instance[FormReaders]
  override protected val dbSession = instance[DBSession]
  override protected val controllerMethod = instance[ControllerMethod]
  override protected val postController = instance[POSTControllerMethod]
  override protected val httpFilters = instance[HttpFilters]

  override protected val blobs = instance[Blobs]
  override protected val cacheFactory = instance[CacheFactory]
  override protected val transactionalMail = instance[TransactionalMail]
  override protected val bulkMailList = instance[BulkMailList]
  override protected val payment = instance[Payment]
  override protected val schema = instance[Schema]
  override protected val featured = instance[Featured] 
  override protected val signupModal = instance[SignupModal]
  override protected val formConstraints = instance[FormConstraints]

  override protected val accountStore = instance[AccountStore]
  override protected val administratorStore = instance[AdministratorStore]
  override protected def categoryStore = instance[CategoryStore]
  override protected def categoryValueStore = instance[CategoryValueStore]
  override protected def celebrityRequestStore = instance[CelebrityRequestStore]
  override protected val celebrityStore = instance[CelebrityStore]
  override protected val checkoutServices = instance[CheckoutServices]
  override protected val couponStore = instance[CouponStore]
  override protected val customerStore = instance[CustomerStore]
  override protected val egraphStore = instance[EgraphStore]
  override protected val enrollmentBatchStore = instance[EnrollmentBatchStore]
  override protected val inventoryBatchStore = instance[InventoryBatchStore]
  override protected val mastheadStore = instance[MastheadStore]
  override protected val mastheadCategoryValueStore = instance[MastheadCategoryValueStore]
  override protected val lineItemStore = instance[LineItemStore]
  override protected val orderStore = instance[OrderStore]
  override protected val printOrderStore = instance[PrintOrderStore]
  override protected val productStore = instance[ProductStore]
  override protected val verticalStore = instance[VerticalStore]
  override protected val videoAssetStore = instance[VideoAssetStore]
  override protected val videoAssetCelebrityStore = instance[VideoAssetCelebrityStore]
  override protected val marketplaceServices = instance[MarketplaceServices]
  
  override protected val couponQueryFilters = instance[CouponQueryFilters]
  override protected val egraphQueryFilters = instance[EgraphQueryFilters]
  override protected val inventoryBatchQueryFilters = instance[InventoryBatchQueryFilters]
  override protected val orderQueryFilters = instance[OrderQueryFilters]
  override protected val printOrderQueryFilters = instance[PrintOrderQueryFilters]

  override protected val checkouts = instance[CheckoutAdapterServices]
}
