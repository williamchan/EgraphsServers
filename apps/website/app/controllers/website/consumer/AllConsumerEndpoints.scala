package controllers.website.consumer

import play.api.mvc.Controller
import controllers.website._

/** All endpoints for the recent consumer website */
trait AllConsumerEndpoints
  extends GetBlobEndpoint
  with GetAccountSettingsEndpoint
  with GetCustomerGalleryEndpoint
  with GetEgraphEndpoint
  with GetEgraphExplanationCardEndpoint
  with GetMarketplaceEndpoint
  with GetOrderConfirmationEndpoint
  with GetLoginEndpoint
  with GetRecoverAccountEndpoint
  with GetResetPasswordEndpoint
  with GetStaticEndpoint
  with LogoutEndpoints
  with PostAccountSettingsEndpoint
  with PostBulkEmailController
  with PostLoginEndpoint
  with PostOrderConfigureEndpoint
  with PostRecoverAccountEndpoint
  with PostRequestStarEndpoint
  with PostResetPasswordEndpoint
  with GetFacebookLoginCallbackEndpoint
  with CelebrityLandingConsumerEndpoint
  with PostRegisterConsumerEndpoint
  with StorefrontChoosePhotoConsumerEndpoints
  with StorefrontPersonalizeConsumerEndpoints
  with StorefrontReviewConsumerEndpoints
  with StorefrontCheckoutConsumerEndpoints
  with StorefrontFinalizeConsumerEndpoints
  with StorefrontFailedConsumerEndpoints
  with PurchaseFlowEndpoints
{ this: Controller => }
