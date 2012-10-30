package services.http.filters

import com.google.inject.Inject
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import play.api.mvc.Session
import controllers.WebsiteControllers
import models.AdministratorStore
import models.Account
import models.Celebrity
import models.CelebrityStore
import models.Product
import models.ProductQueryFilters
import models.enums.PublishedStatus
import services.http.EgraphsSession

/**
 * Filter for taking a product url slug and a celebrity and returning a product if the former
 * were deemed valid, otherwise `404-NotFound`.
 */
class RequireProductUrlSlug @Inject() (productFilters: ProductQueryFilters) extends Filter[(String, Celebrity), Product] {

  override def filter(urlSlugAndCelebrity: (String, Celebrity)): Either[Result, Product] = {
    val (urlSlug, celebrity) = urlSlugAndCelebrity

    celebrity.products(productFilters.byUrlSlug(urlSlug)).headOption match {
      case None => Left(productNotFoundResult(celebrity.publicName, urlSlug))
      case Some(product) => Right(product)
    }
  }

  private def productNotFoundResult(celebName: String, productUrlSlug: String): Result = {
    NotFound(celebName + " doesn't have any product with url " + productUrlSlug)
  }
}