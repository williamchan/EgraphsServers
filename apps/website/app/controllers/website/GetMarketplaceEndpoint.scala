package controllers.website.consumer

import play.api._
import play.api.mvc.Action
import play.api.mvc.Controller
import models._
import models.frontend.marketplace._
import play.api.data._
import play.api.data.Forms._
import controllers.WebsiteControllers
import enums.{PrivacyStatus, EgraphState}
import models.frontend.egraphs._
import services.mvc.ImplicitHeaderAndFooterData
import models.GalleryOrderFactory
import services.ConsumerApplication
import services.http.{SafePlayParams, ControllerMethod}
import services.http.EgraphsSession.Conversions._
import services.http.filters._
import egraphs.authtoken.AuthenticityToken

/**
 * Controller for serving the celebrity marketplace
 */

private[controllers] trait GetMarketplaceEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod : ControllerMethod
  
  import SafePlayParams.Conversions._
  val queryUrl = controllers.routes.WebsiteControllers.getMarketplaceResultPage.url
  val categoryRegex = new scala.util.matching.Regex("""c([0-9])""", "id")
  
  def getMarketplaceVerticalPage(verticalname: String) =  controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
     //Map Vertical to specific landing page.
       Ok(views.html.frontend.marketplace_landing(
         queryUrl = queryUrl.toString,
         verticalViewModels = List[VerticalViewModel](),
         results = List[ResultSetViewModel](),
         categoryViewModels = List[CategoryViewModel]()
       ))
      
    }
  }
    
  def getMarketplaceResultPage = controllerMethod.withForm() { implicit AuthToken => 
    Action { implicit request =>
      // Determine what search options, if any, have been appended
      val verticalOption = Form("vertical" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      val queryOption = Form("query" -> nonEmptyText).bindFromRequest.fold(formWithErrors => None, validForm => Some(validForm))
      
      val categoryAndCategoryValues  = for((key, set) <- request.queryString) yield {
        categoryRegex findFirstIn key match {
          case Some(categoryRegex(id)) => (id, set)
          case None => None
        }
      }      
      // 3) No results, send garbage
      // 4) Error in data, same as no results but with error message
      
       Ok(views.html.frontend.marketplace_results(
         queryUrl = queryUrl.toString,
         verticalViewModels = List[VerticalViewModel](),
         results = List[ResultSetViewModel](),
         categoryViewModels = List[CategoryViewModel]()
       ))
    }
  }

}