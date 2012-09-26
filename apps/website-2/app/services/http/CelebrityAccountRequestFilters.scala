package services.http

import play.api.mvc.Request
import models._
import com.google.inject.Inject
import enums.PublishedStatus
import play.api.mvc.Results.{Forbidden, NotFound}
import java.util.Properties
import controllers.WebsiteControllers
import play.api.mvc.Session
import play.api.mvc.Result

/**
 * Functions that filter out whose callback parameters are only called when the egraphs
 * database found Celebrity and Products that matched provided information in
 * the request.
 */
class CelebrityAccountRequestFilters @Inject() (
  celebStore: CelebrityStore,
  accountFilters: AccountRequestFilters,
  productFilters: ProductQueryFilters,
  administratorStore: AdministratorStore,
  @PlayConfig playConfig: Properties
)
{
  import SafePlayParams.Conversions._

  /**
   * Requires that the request contain a "celebrityId" param that corresponds to
   * a celebrity that actually exists.
   * 
   * @param request the request to be checked for existence of the celebrity ID
   * @param continue the code to execute once the celebrity is found
   *
   * @return either the result of continue or a new NotFound.
   */
//  @deprecated("instead use RequireCelebrityId.inRequest")
//  def requireCelebrityId(request: Request)(continue: Celebrity => Any) = {    
//    val celebrityIdParamOption = request.params.getOption("celebrityId")
//    val celebrityOption = celebrityIdParamOption.flatMap { celebrityIdParam =>
//      celebStore.findById(celebrityIdParam.toLong)
//    }
//    
//    celebrityOption match {
//      case Some(celebrity) => continue(celebrity)
//      case None => new NotFound("No such celebrity")
//    }
//  }
  
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
  def requireCelebrityProductUrl(celebrity: Celebrity, productUrlSlug: String, session: Session)(continue: Product => Any) = {
    celebrity.products(productFilters.byUrlSlug(productUrlSlug)).headOption match {
      case None =>
        NotFound(celebrity.publicName + " doesn't have any product with url " + productUrlSlug)

      case Some(product) if !isProductViewable(product) =>
        NotFound(celebrity.publicName + " doesn't have any product with url " + productUrlSlug)

      case Some(product)  =>
        continue(product)
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
  def requireCelebrityAndProductUrlSlugs(
      celebrityUrlSlug: String, 
      productUrlSlug: String, 
      session: Session
  )(
      continue: (Celebrity, Product) => Any
  ) = {
   requireCelebrityUrlSlug(celebrityUrlSlug, session) { celebrity =>
      requireCelebrityProductUrl(celebrity, productUrlSlug, session) { product =>
        continue(celebrity, product)
      }
    }
  }

  /**
   * @return true either if Celebrity is Published or if full admin tools are enabled and admin is logged in, else false
   */
  protected[http] def isCelebrityViewable(celebrity: Celebrity, session: Session): Boolean = {
    (celebrity.publishedStatus == PublishedStatus.Published) || (isAdminToolsFullyEnabled && isAdmin(session))
  }

  /**
   * @return true either if Product is Published or if full admin tools are enabled and admin is logged in, else false
   */
  protected[http] def isProductViewable(product: Product, session:Session): Boolean = {
    (product.publishedStatus == PublishedStatus.Published) || (isAdminToolsFullyEnabled && isAdmin(session))
  }

  protected[http] def isAdminToolsFullyEnabled: Boolean = {
    playConfig.getProperty("admin.tools.enabled") == "full"
  }

  private def isAdmin(session: Session): Boolean = {   
    administratorStore.isAdmin(adminId = session.getLongOption(WebsiteControllers.adminIdKey))
  }
}
