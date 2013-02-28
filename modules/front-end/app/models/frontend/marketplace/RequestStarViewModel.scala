package models.frontend.marketplace

/**
 * ViewModel for rendering the request a star form on the marketplace.
 *
 * See [[views.html.frontend.marketplace.result_set]]
 *
 * @param starName the requested star name as submitted
 */
case class RequestStarViewModel(
  starName: String
)