package controllers.website.admin

import play.api.mvc.Controller

/**
 * All the endpoints associated with the admin console
 */
trait AllAdminEndpoints
  extends GetRootAdminEndpoint
  with GetAccountAdminEndpoint
  with PostAccountAdminEndpoint
  with GetAccountsAdminEndpoint
  with GetCelebritiesAdminEndpoint
  with GetCelebrityAdminEndpoint
  with GetCelebrityEgraphsAdminEndpoint
  with GetCelebrityInventoryBatchesAdminEndpoint
  with GetCelebrityOrdersAdminEndpoint
  with GetCelebrityProductsAdminEndpoint
  with GetCreateCelebrityInventoryBatchAdminEndpoint
  with GetCreateCelebrityProductAdminEndpoint
  with GetEgraphAdminEndpoint
  with GetEgraphsAdminEndpoint
  with GetFilterAdminEndpoint
  with GetFilterValueAdminEndpoint
  with PostFilterAdminEndpoint
  with PostFilterValueAdminEndpoint
  with GetFiltersAdminEndpoint
  with GetInventoryBatchAdminEndpoint
  with GetLoginAdminEndpoint
  with GetOrderAdminEndpoint
  with GetOrdersAdminEndpoint
  with GetPrintOrderAdminEndpoint
  with GetPrintOrdersAdminEndpoint
  with GetProductAdminEndpoint
  with GetReportsAdminEndpoint
  with GetToolsAdminEndpoint
  with PostCelebrityAdminEndpoint
  with PostCelebrityFilterValueAdminEndpoint
  with PostInventoryBatchAdminEndpoint
  with PostProductAdminEndpoint
  with PostEgraphAdminEndpoint
  with PostLoginAdminEndpoint
  with PostOrderAdminEndpoint
  with PostPrintOrderAdminEndpoint
  with PostSendCelebrityWelcomeEmailAdminEndpoint
  with PostFeaturedCelebritiesAdminEndpoint
{ this: Controller => }
