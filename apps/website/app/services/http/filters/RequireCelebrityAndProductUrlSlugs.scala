package services.http.filters

import com.google.inject.Inject

import models.Celebrity
import models.Product
import play.api.mvc.BodyParsers.parse
import play.api.mvc.BodyParser
import play.api.mvc.Request
import play.api.mvc.Action
import play.api.mvc.Result

/**
 * This requires a celebrity and product url slug to exist.  They must also be published, unless
 * there is an admin.
 *
 * NOTE: This is not a good example of how to compose filters.  I'm a little unhappy how this
 * class became really big, but it does take the magic out of RequireCelebrityUrlSlug and
 * RequireProductUrlSlug allowing them to be extend Filter.
 */
class RequireCelebrityAndProductUrlSlugs @Inject() (
  requireCelebrityUrlSlug: RequireCelebrityUrlSlug,
  requireCelebrityPublished: RequireCelebrityPublished,
  requireProductUrlSlug: RequireProductUrlSlug,
  requireProductPublished: RequireProductPublished,
  requireAdministratorLogin: RequireAdministratorLogin) {

  def apply[A](celebrityUrlSlug: String, productUrlSlug: String, parser: BodyParser[A] = parse.anyContent)(actionFactory: (Celebrity, Product) => Action[A]): Action[A] = {
    requireCelebrityUrlSlug(celebrityUrlSlug, parser) { celebFromId =>
      requireProductUrlSlug((productUrlSlug, celebFromId), parser) { productFromId =>
        requireAdministratorLogin.inSessionOrUseOtherFilter(celebFromId, parser)(otherFilter = requireCelebrityPublished.filter(celebFromId)) { celeb =>
          requireAdministratorLogin.inSessionOrUseOtherFilter(productFromId, parser)(otherFilter = requireProductPublished.filter(productFromId)) { product =>
            Action(parser) { request =>
              actionFactory(celeb, product).apply(request)
            }
          }
        }
      }
    }
  }

  def asOperationalResult[A, T](celebrityUrlSlug: String, productUrlSlug: String, parser: BodyParser[A] = parse.anyContent)(operation: (Celebrity, Product) => T)(implicit request: Request[A]): Either[Result, T] = {
    val resultOrPublishedCelebrityAndProduct = filter(celebrityUrlSlug, productUrlSlug, parser)
    resultOrPublishedCelebrityAndProduct.fold(
      result => Left(result),
      celebrityAndProduct => Right(operation(celebrityAndProduct._1, celebrityAndProduct._2)))
  }

  def filter[A](celebrityUrlSlug: String, productUrlSlug: String, parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, (Celebrity, Product)] = {
    val errorOrAdmin = requireAdministratorLogin.filterInSession(parser)
    val isAdmin = errorOrAdmin.isRight

    val resultOrMaybePublishedCelebrityAndProduct = for {
      maybePublishedCeleb <- requireCelebrityUrlSlug.filter(celebrityUrlSlug).right
      maybePublishedProduct <- requireProductUrlSlug.filter((productUrlSlug, maybePublishedCeleb)).right
    } yield {
      (maybePublishedCeleb, maybePublishedProduct)
    }

    val errorOrResult = for {
      celebrityAndProduct <- resultOrMaybePublishedCelebrityAndProduct.right // this can be fixed to (celebrity, product) in Scala 2.10 
      publishedCelebrity <- requireCelebrityPublished.filter(celebrityAndProduct._1).right
      publishedProduct <- requireProductPublished.filter(celebrityAndProduct._2).right
    } yield {
      (publishedCelebrity, publishedProduct)
    }

    if (isAdmin && errorOrResult.isLeft && resultOrMaybePublishedCelebrityAndProduct.isRight) {
      resultOrMaybePublishedCelebrityAndProduct
    } else {
      errorOrResult
    }
  }
}