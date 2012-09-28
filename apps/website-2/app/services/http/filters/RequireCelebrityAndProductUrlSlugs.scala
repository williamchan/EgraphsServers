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
class RequireCelebrityAndProductUrlSlugs @Inject() (
  requireCelebrityUrlSlug: RequireCelebrityUrlSlug,
  requireProductUrlSlug: RequireProductUrlSlug
) {
  
  def apply[A](celebrityUrlSlug: String, productUrlSlug: String, parser: BodyParser[A] = parse.anyContent)
  (actionFactory: (Celebrity, Product) => Action[A])
  : Action[A] = 
  {
    requireCelebrityUrlSlug(celebrityUrlSlug, parser=parser) { celeb =>
      requireProductUrlSlug(celeb, productUrlSlug, parser=parser) { product =>
        Action(parser) { request =>
          actionFactory(celeb, product).apply(request)
        }
      }
    }
  }
   
  def asOperationResult[A](celebrityUrlSlug: String, productUrlSlug: String, session: Session)
  (operation: (Celebrity, Product) => A)
  : Either[Result, A] =
  {
    for (
      celeb <- requireCelebrityUrlSlug.asOperationResult(celebrityUrlSlug, session)(celeb => celeb).right;
      product <- requireProductUrlSlug.asOperationResult(celeb, productUrlSlug, session)(product => product).right
    ) yield {
      operation(celeb, product)
    }
  }
}