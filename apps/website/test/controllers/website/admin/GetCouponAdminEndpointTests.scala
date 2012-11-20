package controllers.website.admin

import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import utils.AdminProtectedResourceTests

class GetCouponAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getCouponAdmin(1)
}

class GetCreateCouponAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getCreateCouponAdmin
}

class GetCouponsAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getCouponsAdmin
}
