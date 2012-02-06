package services.http

import play.mvc.Http.Request
import models._
import com.google.inject.Inject
import play.mvc.results.{Forbidden, NotFound}

/**
 * Functions that filter out whose callback parameters are only called when the egraphs
 * database found Celebrity and Products that matched provided information in
 * the request.
 */
class CelebrityAccountRequestFilters @Inject() (
  celebStore: CelebrityStore,
  accountFilters: AccountRequestFilters,
  productFilters: ProductQueryFilters)
{
  import OptionParams.Conversions._

  /**
   * Filters out requests that didn't provide valid login/password credentials for an [[models.Account]]
   * with a [[models.Celebrity]] face.
   *
   * Calls the `continue` callback parameter if the filter passed, parameterized with the corresponding
   * Account and Celebrity.
   *
   * @param continue function to call if the request passed the filter
   * @param request the request whose params should be checked by the filter
   *
   * @return the return value of `continue` if the filter passed, otherwise `403-Forbidden`.
   */
  def requireCelebrityAccount(continue: (Account, Celebrity) => Any)(implicit request: Request) = {
    accountFilters.requireAuthenticatedAccount { account =>
      request.params.getOption("celebrityId") match {
        case None =>
          new Forbidden("Celebrity ID was required but not provided")

        case Some(celebrityId) if celebrityId == "me" =>
          account.celebrityId match {
            case None =>
              new Forbidden("This request requires a celebrity account.")

            case Some(accountCelebrityId) =>
              continue(account, celebStore.get(accountCelebrityId))
          }

        case Some(celebrityId) =>
          new Forbidden(
            "Unexpected request for celebrityId \""+celebrityId+"\". Only \"me\" is currently supported."
          )
      }
    }
  }

  /**
   * Filters out requests that didn't provide a valid `celebrityUrlSlug` parameter.
   *
   * Calls the `continue` callback parameter with the corresponding [[models.Celebrity]] if the filter
   * passed.
   *
   * @param continue function to call if the request passed the filter
   * @param request the request whose params should be checked by the filter
   *
   * @return the return value of continue if the filter passed, otherwise `403-Forbidden`
   */
  def requireCelebrityUrlSlug(continue: Celebrity => Any)(implicit request:Request) = {
    request.params.getOption("celebrityUrlSlug") match {
      case None =>
        throw new IllegalStateException(
          """
          celebrityUrlSlug parameter was not provided. This should never have happened since our routes are supposed
          to ensure that it is present in the url that maps to this controller.
          """
        )

      case Some(celebrityUrlSlug) =>
        celebStore.findByUrlSlug(celebrityUrlSlug) match {
          case None =>
            new NotFound("No celebrity with url \"" + celebrityUrlSlug + "\"")

          case Some(celebrity) =>
            continue(celebrity)
        }
    }
  }

  /**
   * Filters out requests that didn't provide a valid `productUrlSlug` parameter for the parameterized
   * [[models.Celebrity]].
   *
   * Calls the `continue` callback parameter with the corresponding [[models.Product]] if the filter passed.
   *
   * @param continue function to call if the request passed the filter
   * @param request the request whose params should be checked by the filter
   *
   * @return the return value of `continue` if the filter passed, otherwise `404-NotFound`.
   */
  def requireCelebrityProductUrl(celebrity: Celebrity)(continue: Product => Any)(implicit request:Request) = {
    request.params.getOption("productUrlSlug") match {
      case None =>
        throw new IllegalStateException(
          """
          productUrlSlug parameter not found. This should never have happened since our routes are supposed
          to ensure that the parameter is present.
          """
        )

      case Some(productUrlSlug) =>
        celebrity.products(productFilters.byUrlSlug(productUrlSlug)).headOption match {
          case None =>
            new NotFound(celebrity.publicName.get + " doesn't have any product with url " + productUrlSlug)

          case Some(product) =>
            continue(product)
        }
    }
  }

  /**
   * Filters out requests that didn't provide valid [[models.Celebrity]]/[[models.Product]] url slug
   * combinations.
   *
   * Calls the `continue` callback parameter only if the filter passed.
   *
   * @param continue function to call if the request passed the filter
   * @param request the request whose params should be checked by the filter
   *
   * @return the return value of `continue` if the filter passed, otherwise `404-NotFound`
   */
  def requireCelebrityAndProductUrlSlugs(continue: (Celebrity, Product) => Any)(implicit request: Request) = {
    requireCelebrityUrlSlug { celebrity =>
      requireCelebrityProductUrl(celebrity) { product =>
        continue(celebrity, product)
      }
    }
  }
}