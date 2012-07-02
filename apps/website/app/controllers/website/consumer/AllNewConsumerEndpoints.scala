package controllers.website.consumer

import play.mvc.Controller

/** All endpoints for the consumer website */
trait AllNewConsumerEndpoints
  extends GetStorefrontChoosePhotoConsumerEndpoint
  with PostStorefrontChoosePhotoConsumerEndpoint
  with StorefrontPersonalizeConsumerEndpoints
  with GetStorefrontReviewEndpoint
{ this: Controller => }
