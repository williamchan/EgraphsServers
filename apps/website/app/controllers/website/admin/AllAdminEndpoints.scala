package controllers.website.admin

import play.api.mvc.Controller

/**
 * All the endpoints associated with the admin console
 */
trait AllAdminEndpoints
  extends GetRootAdminEndpoint
  with GetAccountAdminEndpoint
  with GetAccountsAdminEndpoint
  with GetCategoryAdminEndpoint
  with GetCategoriesAdminEndpoint
  with GetCategoryValueAdminEndpoint
  with GetCelebritiesAdminEndpoint
  with GetCelebrityAdminEndpoint
  with GetCelebrityEgraphsAdminEndpoint
  with GetCreateFreegraphAdminEndpoint
  with GetCelebrityInventoryBatchesAdminEndpoint
  with GetCelebrityOrdersAdminEndpoint
  with GetCelebrityProductsAdminEndpoint
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
  with GetVideoEnrollmentAdminEndpoint
  with PostAccountAdminEndpoint
  with PostCategoryAdminEndpoint
  with PostCategoryValueAdminEndpoint
  with PostCelebrityAdminEndpoint
  with PostCelebrityCategoryValueAdminEndpoint
  with PostEgraphAdminEndpoint
  with PostFeaturedCelebritiesAdminEndpoint
  with PostFreegraphAdminEndpoint
  with PostInventoryBatchAdminEndpoint
  with PostLoginAdminEndpoint
  with PostOrderAdminEndpoint
  with PostPrintOrderAdminEndpoint
  with PostProcessVideoAdminEndpoint
  with PostProductAdminEndpoint
  with PostSendCelebrityWelcomeEmailAdminEndpoint
  with PostVideoAssetAdminEndpoint
{ this: Controller => }
