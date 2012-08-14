package controllers.website.admin

import play.mvc.Controller

/**
 * All the endpoints associated with the admin console
 */
trait AllAdminEndpoints
  extends GetRootAdminEndpoint
  with GetAccountAdminEndpoint
  with GetAccountsAdminEndpoint
  with GetCelebritiesAdminEndpoint
  with GetCelebrityAdminEndpoint
  with GetCelebrityEgraphsAdminEndpoint
  with GetCelebrityInventoryBatchesAdminEndpoint
  with GetCelebrityOrdersAdminEndpoint
  with GetCelebrityProductsAdminEndpoint
  with GetCreateAccountAdminEndpoint
  with GetCreateCelebrityAdminEndpoint
  with GetCreateCelebrityInventoryBatchAdminEndpoint
  with GetCreateCelebrityProductAdminEndpoint
  with GetEgraphAdminEndpoint
  with GetEgraphsAdminEndpoint
  with GetInventoryBatchAdminEndpoint
  with GetLoginAdminEndpoint
  with GetOrderAdminEndpoint
  with GetOrdersAdminEndpoint
  with GetPrintOrderAdminEndpoint
  with GetPrintOrdersAdminEndpoint
  with GetProductAdminEndpoint
  with GetReportsAdminEndpoint
  with GetToolsAdminEndpoint
  with PostAccountAdminEndpoint
  with PostCelebrityAdminEndpoint
  with PostCelebrityInventoryBatchAdminEndpoint
  with PostCelebrityProductAdminEndpoint
  with PostEgraphAdminEndpoint
  with PostLoginAdminEndpoint
  with PostOrderAdminEndpoint
  with PostPrintOrderAdminEndpoint
  with PostFeaturedCelebritiesAdminEndpoint
{ this: Controller => }
