package utils

import com.google.inject.Inject
import services.http._
import forms.purchase.{PurchaseFormChecksFactory, FormReaders, PurchaseFormFactory}
import services.mail.Mail
import services.payment.Payment
import models.{InventoryBatchQueryFilters, EgraphQueryFilters, OrderQueryFilters}
import services.db.DBSession
import play.mvc.Controller
import controllers.website.AllWebsiteEndpoints
import java.util.Properties
import forms._
import play.mvc.Scope.{Session, Flash}
import play.mvc.Http.Request
import play.test.FunctionalTest
import services.AppConfig
import services.mvc.StorefrontBreadcrumbData

/**
 * Injectable version of AllWebsiteEndpoints with configurable session, flash,
 * and request.
 */
case class TestWebsiteControllers @Inject()(
  controllerMethod: ControllerMethod,
  postController: POSTControllerMethod,
  accountRequestFilters: AccountRequestFilters,
  adminFilters: AdminRequestFilters,
  celebFilters: CelebrityAccountRequestFilters,
  customerFilters: CustomerRequestFilters,
  mail: Mail,
  payment: Payment,
  orderQueryFilters: OrderQueryFilters,
  egraphQueryFilters: EgraphQueryFilters,
  inventoryBatchQueryFilters: InventoryBatchQueryFilters,
  dbSession: DBSession,
  @PlayConfig playConfig: Properties,
  facebookAppId: String,
  customerLoginForms: CustomerLoginFormFactory,
  accountSettingsForms: AccountSettingsFormFactory,
  accountVerificationForms: AccountVerificationFormFactory,
  accountRecoverForms: AccountRecoverFormFactory,
  egraphsSessionFactory: () => EgraphsSession,
  fakeRequest: Request = FunctionalTest.newRequest(),
  fakeSession: Session = new Session(),
  fakeFlash: Flash = new Flash()
)() extends Controller with AllWebsiteEndpoints {
  val breadcrumbData = AppConfig.instance[StorefrontBreadcrumbData]
  val checkPurchaseField = AppConfig.instance[PurchaseFormChecksFactory]
  val purchaseFormFactory = AppConfig.instance[PurchaseFormFactory]
  val formReaders = AppConfig.instance[FormReaders]
  val formChecks = AppConfig.instance[FormChecks]
  override def request = fakeRequest
  override def params = request.params
  override def session = fakeSession
  override def flash = fakeFlash
}
