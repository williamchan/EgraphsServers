package utils

import com.google.inject.Inject
import services.http._
import forms.purchase.{PurchaseFormChecksFactory, FormReaders, PurchaseFormFactory}
import services.mail.{BulkMailList, TransactionalMail}
import services.payment.Payment
import models._
import services.db.DBSession
import play.api.mvc.Controller
import controllers.website.AllWebsiteEndpoints
import java.util.Properties
import forms._
import services.{Utils, AppConfig}
import services.mvc.celebrity.CatalogStarsQuery
import services.mvc.{OrderCompleteViewModelFactory, StorefrontBreadcrumbData}
import services.http.filters.HttpFilters
import play.api.test.FakeRequest
import play.api.mvc.Request
import play.api.mvc.AnyContent
import services.config.ConfigFileProxy
import services.blobs.Blobs
import services.db.Schema

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
  orderQueryFilters: OrderQueryFilters,
  egraphQueryFilters: EgraphQueryFilters,
  inventoryBatchQueryFilters: InventoryBatchQueryFilters,
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
  customerLoginForms: CustomerLoginFormFactory,
  egraphsSessionFactory: () => EgraphsSession,
  fakeRequest: Request[AnyContent] = FakeRequest()
)() extends Controller with AllWebsiteEndpoints {
  import AppConfig.instance
  
  override def enrollmentBatchStore = instance[EnrollmentBatchStore]
  override def schema = instance[Schema]
  override def inventoryBatchStore = instance[InventoryBatchStore]
  override def egraphStore = instance[EgraphStore]  
  override def accountSettingsForms = instance[AccountSettingsFormFactory]
  override def accountPasswordResetForms = instance[AccountPasswordResetFormFactory]
  override def accountRecoverForms = instance[AccountRecoverFormFactory]
  override def blobs = instance[Blobs]
  override def bulkMailList = instance[BulkMailList]
  override def breadcrumbData = instance[StorefrontBreadcrumbData]
  override def checkPurchaseField = instance[PurchaseFormChecksFactory]
  override def purchaseFormFactory = instance[PurchaseFormFactory]
  override def formReaders = instance[FormReaders]  

  override def catalogStarsQuery = instance[CatalogStarsQuery]
  override def orderCompleteViewModelFactory = instance[OrderCompleteViewModelFactory]
  override def printOrderQueryFilters = instance[PrintOrderQueryFilters]  
}
