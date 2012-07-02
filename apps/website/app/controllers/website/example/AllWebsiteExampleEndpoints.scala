package controllers.website.example

import play.mvc.Controller

/**
 * All endpoints that are non-central, but maintained for purposes of example
 */
trait AllWebsiteExampleEndpoints extends GetSocialPostEndpoint { this: Controller => }
