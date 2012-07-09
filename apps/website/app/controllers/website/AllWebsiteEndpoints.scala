package controllers.website

import admin.AllAdminEndpoints
import consumer.{GetRootConsumerEndpoint, AllConsumerEndpoints}
import play.mvc.Controller
import controllers.website.nonproduction.PostBuyDemoProductEndpoint

/**
 * All endpoints for the website (doesn't include the API)
 */
trait AllWebsiteEndpoints
  extends GetRootConsumerEndpoint
  with AllConsumerEndpoints
  with AllAdminEndpoints
  with PostBuyDemoProductEndpoint
{ this: Controller =>}
