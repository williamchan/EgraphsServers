package controllers.website.consumer

import play.mvc.Controller

/** All endpoints for the recent consumer website */
trait AllNewConsumerEndpoints
  extends GetRootConsumerEndpoint
  with StorefrontChoosePhotoConsumerEndpoints
  with StorefrontPersonalizeConsumerEndpoints
  with StorefrontReviewConsumerEndpoints
  with StorefrontCheckoutConsumerEndpoints
  with StorefrontFinalizeConsumerEndpoints
{ this: Controller => }
