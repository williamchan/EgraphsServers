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
  with PostBuyProductEndpoint { this: Controller => }