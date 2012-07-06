package controllers.website

import play.mvc.Controller

/**
 * All endpoints associated with the consumer-facing website
 */
trait AllConsumerEndpoints extends GetBlobEndpoint
  with GetAccountSettingsEndpoint
  with GetCelebrityEndpoint
  with GetCelebrityProductEndpoint
  with GetCustomerGalleryEndpoint
  with GetEgraphEndpoint
  with GetOrderConfirmationEndpoint
  with GetLoginEndpoint
  with GetRecoverAccountEndpoint
  with GetRecoverAccountConfirmationEndpoint
  with GetRegisterEndpoint
  with GetResetPasswordEndpoint
  with GetStaticEndpoint
  with PostAccountSettingsEndpoint
  with PostBuyProductEndpoint
  with PostLoginEndpoint
  with PostLogoutEndpoint
  with PostOrderConfigureEndpoint
  with PostRecoverAccountEndpoint
  with PostRegisterEndpoint
  with PostResetPasswordEndpoint
  with GetFacebookLoginCallbackEndpoint{ this: Controller => }