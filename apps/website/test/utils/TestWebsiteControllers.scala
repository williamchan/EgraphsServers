package utils

import com.google.inject.Inject
import services.http._
import forms.purchase.{PurchaseFormChecksFactory, FormReaders, PurchaseFormFactory}
import services.mail.{BulkMailList, TransactionalMail}
import services.payment.Payment
import models._
import checkout.{CheckoutServices, LineItemStore, CheckoutAdapterServices}
import models.categories._
import services.db.DBSession
import play.api.mvc.Controller
import controllers.website.AllWebsiteEndpoints
import java.util.Properties
import forms._
import services.{AppConfig, ConsumerApplication, Utils}
import services.mvc.celebrity.CatalogStarsQuery
import services.mvc.{OrderCompleteViewModelFactory, StorefrontBreadcrumbData}
import services.http.filters.HttpFilters
import play.api.test.FakeRequest
import play.api.mvc.Request
import play.api.mvc.AnyContent
import services.config.ConfigFileProxy
import services.blobs.Blobs
import services.db.Schema
import services.mvc.marketplace.MarketplaceServices
import services.mvc.landing.LandingMastheadsQuery
import services.cache.CacheFactory

/**
 * Injectable version of AllWebsiteEndpoints with configurable session, flash,
 * and request.
 */
case class TestWebsiteControllers @Inject()(
  controllerMethod: ControllerMethod,
  postController: POSTControllerMethod,
  httpFilters: HttpFilters,
  transactionalMail: TransactionalMail,
  payment: Payment,
  dbSession: DBSession,
  facebookAppId: String,
  accountStore: AccountStore,
  administratorStore: AdministratorStore,
  celebrityStore: CelebrityStore,
  customerStore: CustomerStore,
  config: ConfigFileProxy,
  orderStore: OrderStore,
  printOrderStore: PrintOrderStore,
  productStore: ProductStore,
  videoAssetCelebrityStore: VideoAssetCelebrityStore,
  videoAssetStore: VideoAssetStore,
  egraphsSessionFactory: () => EgraphsSession,
  fakeRequest: Request[AnyContent] = FakeRequest()
)() extends Controller with AllWebsiteEndpoints {
  import AppConfig.instance
  
  override def enrollmentBatchStore = instance[EnrollmentBatchStore]
  override def schema = instance[Schema]
  override def featured = instance[Featured]
  override def cacheFactory = instance[CacheFactory]
  override def verticalStore = instance[VerticalStore]
  override def mastheadStore = instance[MastheadStore]
  override def mastheadCategoryValueStore = instance[MastheadCategoryValueStore]
  override def inventoryBatchStore = instance[InventoryBatchStore]
  override def celebrityRequestStore = instance[CelebrityRequestStore]
  override def egraphStore = instance[EgraphStore]
  override def couponStore = instance[CouponStore]  
  override def accountSettingsForms = instance[AccountSettingsFormFactory]
  override def accountPasswordResetForms = instance[AccountPasswordResetFormFactory]
  override def blobs = instance[Blobs]
  override def bulkMailList = instance[BulkMailList]
  override def breadcrumbData = instance[StorefrontBreadcrumbData]
  override def checkPurchaseField = instance[PurchaseFormChecksFactory]
  override def purchaseFormFactory = instance[PurchaseFormFactory]
  override def formReaders = instance[FormReaders]
  override def marketplaceServices = instance[MarketplaceServices]
  override def signupModal = instance[SignupModal]
  override def formConstraints = instance[FormConstraints]

  override def catalogStarsQuery = instance[CatalogStarsQuery]
  override def landingMastheadsQuery = instance[LandingMastheadsQuery]
  override def checkoutServices = instance[CheckoutServices]
  override def orderCompleteViewModelFactory = instance[OrderCompleteViewModelFactory]
  override def egraphQueryFilters = instance[EgraphQueryFilters]
  override def categoryStore = instance[CategoryStore]
  override def categoryValueStore = instance[CategoryValueStore]
  override def couponQueryFilters = instance[CouponQueryFilters]
  override def inventoryBatchQueryFilters = instance[InventoryBatchQueryFilters]
  override def lineItemStore = instance[LineItemStore]
  override def orderQueryFilters = instance[OrderQueryFilters]
  override def printOrderQueryFilters = instance[PrintOrderQueryFilters]
  override def consumerApp = instance[ConsumerApplication]
  override def checkouts = instance[CheckoutAdapterServices]
}
