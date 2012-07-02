package controllers.website.consumer

import play.mvc.Controller

/** All endpoints for the consumer website */
trait AllNewConsumerEndpoints
  extends StorefrontChoosePhotoConsumerEndpoints
  with StorefrontPersonalizeConsumerEndpoints
  with StorefrontReviewEndpoints
{ this: Controller => }
