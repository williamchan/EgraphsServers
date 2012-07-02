package controllers.website

import admin.AllAdminEndpoints
import example.AllWebsiteExampleEndpoints
import play.mvc.Controller
import controllers.website.nonproduction.PostBuyDemoProductEndpoint

/**
 * All endpoints for the website (doesn't include the API)
 */
trait AllWebsiteEndpoints extends GetRootEndpoint
  with AllConsumerEndpoints
  with AllAdminEndpoints
  with AllWebsiteExampleEndpoints
  with PostBuyDemoProductEndpoint
{ this: Controller =>}
