package models.frontend.marketplace

/**
 * ViewModel for rendering the request a star form on the marketplace.
 *
 * See [[views.html.frontend.marketplace.result_set]]
 *
 * @param starName the requested star name as submitted
 * @param email the email address as submitted
 */
case class RequestStarViewModel(
  starName: String
)