package services.http

import play.mvc.Http.Request
import models._
import com.google.inject.Inject
import play.mvc.results.{Forbidden, NotFound}

// TODO(erem): test and comment this trait
class CelebrityAccountRequestFilters @Inject() (celebStore: CelebrityStore, accountFilters: AccountRequestFilters, productFilters: ProductQueryFilters) {
  import OptionParams.Conversions._

  def requireCelebrityAccount(onAllow: (Account, Celebrity) => Any)(implicit request: Request) = {
    accountFilters.requireAuthenticatedAccount { account =>
      request.params.getOption("celebrityId") match {
        case None =>
          new Forbidden("Celebrity ID was required but not provided")

        case Some(celebrityId) if celebrityId == "me" =>
          account.celebrityId match {
            case None =>
              new Error("This request requires a celebrity account.")

            case Some(accountCelebrityId) =>
              onAllow(account, celebStore.findById(accountCelebrityId).get)
          }

        case Some(celebrityId) =>
          new Forbidden(
            "Unexpected request for celebrityId \""+celebrityId+"\". Only \"me\" is currently supported."
          )
      }
    }
  }

  def requireCelebrityUrlSlug(onAllow: Celebrity => Any)(implicit request:Request) = {
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
            onAllow(celebrity)
        }
    }
  }

  def requireCelebrityProductUrl(celebrity: Celebrity)(onAllow: Product => Any)(implicit request:Request) = {
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
            onAllow(product)
        }
    }
  }

  def requireCelebrityAndProductUrlSlugs(onAllow: (Celebrity, Product) => Any)(implicit request: Request) = {
    requireCelebrityUrlSlug { celebrity =>
      requireCelebrityProductUrl(celebrity) { product =>
        onAllow(celebrity, product)
      }
    }
  }
}