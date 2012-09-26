package services.http.filters

import com.google.inject.Inject
import models.{CelebrityStore, Product}
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import services.http.CelebrityRequest
import models.Account
import services.http.PlayConfig
import models.Celebrity
import java.util.Properties
import play.api.mvc.Session
import models.enums.PublishedStatus
import services.http.SafePlayParams.Conversions._
import models.AdministratorStore
import controllers.WebsiteControllers
import models.ProductQueryFilters


// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireProductUrlSlug @Inject() (
  productStore: CelebrityStore,
  productFilters: ProductQueryFilters,
  adminStore: AdministratorStore,
  @PlayConfig playConfig: Properties
) {
  
 /** NOTE: this is old documentation from when the filter used to live on CelebrityAccountRequestFilters.
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

  def apply[A](celeb: Celebrity, productUrlSlug: String, parser: BodyParser[A] = parse.anyContent)
  (actionFactory: Product => Action[A])
  : Action[A] = 
  {
    Action(parser) { request =>
      val maybeResult = for (
        product <- celeb.products(productFilters.byUrlSlug(productUrlSlug)).headOption.toRight(
                     left=productNotFoundResult(celeb.publicName, productUrlSlug)
                   ).right;
        viewableProduct <- notFoundOrViewableProduct(product, request.session).right
      ) yield {
        actionFactory(viewableProduct).apply(request)
      }
      
      // TODO: PLAY20 migration actually redirect this to the reverse-route of GetLoginCelebEndpoint
      //   instead of returning  a forbidden.
      maybeResult.fold(notFound => notFound, successfulResult => successfulResult)      
    }
  }
  
  //
  // Private members
  //
  private def productNotFoundResult(celebName: String, productUrlSlug: String): Result = {
    NotFound(celebName + " doesn't have any product with url " + productUrlSlug) 
  }
  
  private def notFoundOrViewableProduct(product: Product, session: Session)
  : Either[Result, Product] = 
  {
    val productIsPublished = product.publishedStatus == PublishedStatus.Published
    val viewerIsAdmin = isAdmin(session)
    
    if (productIsPublished || viewerIsAdmin) { 
      Right(product)
    } else {
      Left(NotFound("No photo found with this url"))
    }
  }
   
  private def isAdmin(session: Session): Boolean = {
    val maybeIsAdmin = for (
      adminId <- session.getLongOption(WebsiteControllers.adminIdKey);
      admin <- adminStore.findById(adminId)
    ) yield {
      true
    }
    
    maybeIsAdmin.getOrElse(false)
  }
  
}