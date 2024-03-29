package controllers.website

import mlbpa.MlbpaEndpoints
import play.api.mvc.Controller
import admin.AllAdminEndpoints
import consumer.{GetRootConsumerEndpoint, AllConsumerEndpoints}
import nonproduction.PostBuyDemoProductEndpoint

/**
 * All endpoints for the website (doesn't include the API)
 */
trait AllWebsiteEndpoints
  extends GetRootConsumerEndpoint
  with AllConsumerEndpoints
  with PostBuyDemoProductEndpoint
  with AllAdminEndpoints
  with MlbpaEndpoints
{ this: Controller =>}
