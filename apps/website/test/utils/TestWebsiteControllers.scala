package utils

import com.google.inject.Inject
import services.http._
import services.mail.Mail
import services.payment.Payment
import models.{InventoryBatchQueryFilters, EgraphQueryFilters, OrderQueryFilters}
import services.db.DBSession
import play.mvc.Controller
import controllers.website.AllWebsiteEndpoints
import java.util.Properties
import services.http.forms.{AccountSettingsFormFactory, CustomerLoginFormFactory, FormChecks}
import play.mvc.Scope.{Session, Flash}
import play.mvc.Http.Request
import play.test.FunctionalTest

/**
 * Injectable version of AllWebsiteEndpoints with configurable session, flash,
 * and request.
 */
case class TestWebsiteControllers @Inject()(
  controllerMethod: ControllerMethod,
  postController: POSTControllerMethod,
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
  formChecks: FormChecks,
  customerLoginForms: CustomerLoginFormFactory,
  accountSettingsForms: AccountSettingsFormFactory,
  fakeRequest: Request = FunctionalTest.newRequest(),
  fakeSession: Session = new Session(),
  fakeFlash: Flash = new Flash()
) extends Controller with AllWebsiteEndpoints {
  override def request = fakeRequest
  override def params = request.params
  override def session = fakeSession
  override def flash = fakeFlash
}
