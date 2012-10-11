package controllers.website

import play.api.mvc.Controller
//import admin.AllAdminEndpoints
import consumer.{GetRootConsumerEndpoint, AllConsumerEndpoints}
import controllers.website.nonproduction.PostBuyDemoProductEndpoint

/**
 * All endpoints for the website (doesn't include the API)
 */
trait AllWebsiteEndpoints
  extends GetRootConsumerEndpoint
  with AllConsumerEndpoints
//  with AllAdminEndpoints
  with PostBuyDemoProductEndpoint
{ this: Controller =>}
