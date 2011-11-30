package controllers

import play.mvc.{Before, Controller}
import models.Product

/**
 * Guards Controller classes that mix it in, guaranteeing that a celebrity product's url-slug
 * is present.
 */
trait RequiresCelebrityProductName { this: Controller with RequiresCelebrityName =>
  //
  // Protected members
  //
  /** Access the located product (for use in controller methods only) */
  protected def product: Product = {
    _product.get
  }

  //
  // Private implementation
  //
  private val _product = new ThreadLocal[Product]

  @Before(priority=RequiresCelebrityProductName.interceptPriority)
  protected def ensureProductExists = {
    Option(params.get("productUrlSlug")) match {
      case None =>
        throw new IllegalStateException(
          """
          productUrlSlug parameter not found. This should never have happened since our routes are supposed
          to ensure that the parameter is present.
          """
        )

      case Some(productUrlSlug) =>
        import Product.FindByCelebrity.Filters.WithUrlSlug

        celebrity.products(WithUrlSlug(productUrlSlug)).headOption match {
          case None =>
            NotFound(celebrity.publicName.get + " doesn't have any product with url " + productUrlSlug)

          case Some(product) =>
            _product.set(product)
            Continue
        }
    }
  }
}

object RequiresCelebrityProductName {
  final val interceptPriority = RequiresCelebrityName.interceptPriority + 10
}