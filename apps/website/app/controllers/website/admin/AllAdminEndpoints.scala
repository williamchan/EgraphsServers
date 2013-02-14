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
  with GetCouponAdminEndpoint
  with GetCouponsAdminEndpoint
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
  with GetMastheadsAdminEndpoint
  with GetMastheadAdminEndpoint
  with GetOrderAdminEndpoint
  with GetOrdersAdminEndpoint
  with GetPrintOrderAdminEndpoint
  with GetPrintOrdersAdminEndpoint
  with GetProductAdminEndpoint
  with GetReportsAdminEndpoint
  with GetToolsAdminEndpoint
  with GetTwitterDataAdminEndpoint
  with GetImageAuditAdminEndpoint
  with GetVideoAssetAdminEndpoint
  with PostAccountAdminEndpoint
  with PostCategoryAdminEndpoint
  with PostCategoryValueAdminEndpoint
  with PostCelebrityAdminEndpoint
  with PostCelebrityCategoryValueAdminEndpoint
  with PostCouponAdminEndpoint
  with PostEgraphAdminEndpoint
  with PostFeaturedCelebritiesAdminEndpoint
  with PostFreegraphAdminEndpoint
  with PostInventoryBatchAdminEndpoint
  with PostLoginAdminEndpoint
  with PostMastheadAdminEndpoint
  with PostOrderAdminEndpoint
  with PostPrintOrderAdminEndpoint
  with PostProcessVideoAdminEndpoint
  with PostProductAdminEndpoint
  with PostRebuildSearchIndexAdminEndpoint  
  with PostSendCelebrityWelcomeEmailAdminEndpoint
  with PostVideoAssetAdminEndpoint
{ this: Controller => }
