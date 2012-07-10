package controllers.website.consumer

import play.mvc.Controller
import controllers.website._

/** All endpoints for the recent consumer website */
trait AllConsumerEndpoints
  extends GetBlobEndpoint
  with GetAccountSettingsEndpoint
  with GetCustomerGalleryEndpoint
  with GetEgraphEndpoint
  with GetOrderConfirmationEndpoint
  with GetLoginEndpoint
  with GetRecoverAccountEndpoint
  with GetRecoverAccountConfirmationEndpoint
  with GetResetPasswordEndpoint
  with GetStaticEndpoint
  with LogoutEndpoints
  with PostAccountSettingsEndpoint
  with PostBuyProductEndpoint
  with PostLoginEndpoint
  with PostOrderConfigureEndpoint
  with PostRecoverAccountEndpoint
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
{ this: Controller => }
