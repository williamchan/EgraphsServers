package utils

import com.google.inject.Inject
import services.http.{CelebrityAccountRequestFilters, AdminRequestFilters, POSTControllerMethod, ControllerMethod}
import services.mail.Mail
import services.payment.Payment
import models.{InventoryBatchQueryFilters, EgraphQueryFilters, OrderQueryFilters}
import services.db.DBSession
import play.mvc.Controller
import controllers.website.AllWebsiteEndpoints
import java.util.Properties

/**
 * Injectable version of AllWebsiteEndpoints.
 */
case class TestWebsiteControllers @Inject()(
  controllerMethod: ControllerMethod,
  postController: POSTControllerMethod,
  adminFilters: AdminRequestFilters,
  celebFilters: CelebrityAccountRequestFilters,
  mail: Mail,
  payment: Payment,
  orderQueryFilters: OrderQueryFilters,
  egraphQueryFilters: EgraphQueryFilters,
  inventoryBatchQueryFilters: InventoryBatchQueryFilters,
  dbSession: DBSession,
  playConfig: Properties,
  facebookAppId: String
) extends Controller with AllWebsiteEndpoints
