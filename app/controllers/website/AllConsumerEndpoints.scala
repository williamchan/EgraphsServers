package controllers.website

import play.mvc.Controller

/**
 * All endpoints associated with the consumer-facing website
 */
trait AllConsumerEndpoints extends GetBlobEndpoint
  with GetCelebrityEndpoint
  with GetCelebrityProductEndpoint
  with GetEgraphEndpoint
  with GetOrderConfirmationEndpoint
  with PostBuyProductEndpoint
  with PostLoginEndpoint
  with PostRecoverAccountEndpoint
  with PostRegisterEndpoint
  with PostResetPasswordEndpoint
  with PostFacebookLoginCallbackEndpoint{ this: Controller => }