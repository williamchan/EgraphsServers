package services.ecommerce

import com.google.inject.Inject
import services.http.{ServerSession, ServerSessionFactory}
import play.api.mvc.RequestHeader

/**
 * Gets the user session's cart for a particular storefront.
 *
 * @param sessionFactory
 */
class CartFactory @Inject() (sessionFactory: ServerSessionFactory) {
  def apply(celebId: Long)(implicit request: RequestHeader): ServerSession = {
    sessionFactory(request.session).namespaced("checkouts").namespaced(celebId.toString)
  }
}
