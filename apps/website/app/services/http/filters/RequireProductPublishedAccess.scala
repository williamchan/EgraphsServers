package services.http.filters

import com.google.inject.Inject

import models.enums.PublishedStatus
import models.{CelebrityAccesskey, Product}
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

class RequireProductPublishedAccess @Inject() extends Filter[(Product, String), Product] {
  override def filter(productAndAccesskey: (Product, String)): Either[Result, Product] = {
    val product = productAndAccesskey._1
    val accesskey = productAndAccesskey._2
    if (product.publishedStatus == PublishedStatus.Published
      || CelebrityAccesskey.matchesAccesskey(accesskey, product.celebrityId)) {
      Right(product)
    } else {
      Left(NotFound("No photo found with this url"))
    }
  }
}
