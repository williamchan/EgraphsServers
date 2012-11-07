package services.http.filters

import com.google.inject.Inject

import models.enums.PublishedStatus
import models.Product
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

class RequireProductPublished @Inject() extends Filter[Product, Product] {
  override def filter(Product: Product): Either[Result, Product] = {
    if (Product.publishedStatus == PublishedStatus.Published) {
      Right(Product)
    } else {
      Left(NotFound("No photo found with this url"))
    }
  }
}
