package controllers

import website._
import admin._
import example.{PostFacebookLoginCallbackEndpoint, GetFacebookLoginEndpoint, GetSocialPostEndpoint}
import nonproduction.PostBuyDemoProductEndpoint
import website.GetRootEndpoint
import services.blobs.Blobs
import services.mail.Mail
import services.payment.Payment
import play.mvc.{Router, Controller}
import models._
import services.Utils
import play.mvc.Router.ActionDefinition
import play.mvc.results.Redirect
import services.http.{SecurityRequestFilters, ControllerMethod, AdminRequestFilters, CelebrityAccountRequestFilters}

object WebsiteControllers extends Controller
  with GetRootEndpoint
  with GetBlobEndpoint
  with GetCelebrityEndpoint
  with GetCelebrityProductEndpoint
  with GetEgraphEndpoint
  with GetOrderConfirmationEndpoint
  with PostBuyDemoProductEndpoint
  with PostBuyProductEndpoint

  // admin endpoints
  with GetRootAdminEndpoint
  with GetAccountAdminEndpoint
  with GetAccountsAdminEndpoint
  with GetCelebritiesAdminEndpoint
  with GetCelebrityAdminEndpoint
  with GetCelebrityEgraphsAdminEndpoint
  with GetCelebrityInventoryBatchesAdminEndpoint
  with GetCelebrityOrdersAdminEndpoint
  with GetCelebrityProductsAdminEndpoint
  with GetCreateAccountAdminEndpoint
  with GetCreateCelebrityAdminEndpoint
  with GetCreateCelebrityInventoryBatchAdminEndpoint
  with GetCreateCelebrityProductAdminEndpoint
  with GetEgraphAdminEndpoint
  with GetEgraphsAdminEndpoint
  with GetInventoryBatchAdminEndpoint
  with GetLoginAdminEndpoint
  with GetOrderAdminEndpoint
  with GetOrdersAdminEndpoint
  with GetProductAdminEndpoint
  with GetScriptAdminEndpoint
  with PostAccountAdminEndpoint
  with PostCelebrityAdminEndpoint
  with PostCelebrityInventoryBatchAdminEndpoint
  with PostCelebrityProductAdminEndpoint
  with PostEgraphAdminEndpoint
  with PostLoginAdminEndpoint
  with PostLogoutAdminEndpoint
  with PostOrderAdminEndpoint

  // social media exploratory work
  with GetSocialPostEndpoint
  with GetFacebookLoginEndpoint
  with PostFacebookLoginCallbackEndpoint
{

  import services.AppConfig.instance

  val adminIdKey: String = "admin"

  // Provide endpoint dependencies
  override protected val controllerMethod = instance[ControllerMethod]
  override protected val adminFilters = instance[AdminRequestFilters]
  override protected val securityFilters = instance[SecurityRequestFilters]
  override protected val celebFilters = instance[CelebrityAccountRequestFilters]
  override protected val egraphQueryFilters = instance[EgraphQueryFilters]
  override protected val inventoryBatchQueryFilters = instance[InventoryBatchQueryFilters]
  override protected val orderQueryFilters = instance[OrderQueryFilters]

  override protected val blobs = instance[Blobs]
  override protected val mail = instance[Mail]
  override protected val payment = instance[Payment]

  override protected val accountStore = instance[AccountStore]
  override protected val administratorStore = instance[AdministratorStore]
  override protected val celebrityStore = instance[CelebrityStore]
  override protected val customerStore = instance[CustomerStore]
  override protected val egraphStore = instance[EgraphStore]
  override protected val inventoryBatchStore = instance[InventoryBatchStore]
  override protected val orderStore = instance[OrderStore]
  override protected val productStore = instance[ProductStore]

  def redirectWithValidationErrors(redirectUrl: Router.ActionDefinition, permanent: Option[Boolean] = None): Redirect = {
    // Redirect to redirectUrl, providing field errors via the flash scope.
    import scala.collection.JavaConversions._
    val fieldNames = validationErrors.map {
      case (fieldName, _) => fieldName
    }
    val errorString = fieldNames.mkString(",")
    flash += ("errors" -> errorString)
    params.allSimple().foreach {
      param => flash += param
    }

    if (permanent.isDefined) {
      Redirect(redirectUrl.url, permanent.get)
    } else {
      Redirect(redirectUrl.url)
    }
  }

  def updateFlashScopeWithPagingData[A](pagedQuery: (Iterable[A], Int, Option[Int]), baseUrl: ActionDefinition) {
    val curPage = pagedQuery._2
    val totalResults = pagedQuery._3

    val showPaging = totalResults.isDefined && totalResults.get > Utils.defaultPageLength
    flash.put("ShowPaging", showPaging)
    val totalResultsStr = if (totalResults.isDefined) ("- " + totalResults.get + " results") else ""
    flash.put("TotalResultsStr", totalResultsStr)

    if (showPaging) {
      val showFirst: Boolean = curPage > 2
      flash.put("ShowFirst", showFirst)
      if (showFirst) flash.put("FirstUrl", withPageQuery(baseUrl, 1)) else flash.remove("FirstUrl")

      val showPrev: Boolean = curPage > 1
      flash.put("ShowPrev", showPrev)
      if (showPrev) flash.put("PrevUrl", withPageQuery(baseUrl, curPage - 1)) else flash.remove("PrevUrl")

      val totalNumPages = if (totalResults.get % Utils.defaultPageLength > 0) {
        totalResults.get / Utils.defaultPageLength + 1
      } else {
        totalResults.get / Utils.defaultPageLength
      }

      val showNext: Boolean = curPage < totalNumPages
      flash.put("ShowNext", showNext)
      if (showNext) flash.put("NextUrl", withPageQuery(baseUrl, curPage + 1)) else flash.remove("NextUrl")

      val showLast: Boolean = curPage < totalNumPages
      flash.put("ShowLast", showLast)
      if (showLast) flash.put("LastUrl", withPageQuery(baseUrl, totalNumPages)) else flash.remove("LastUrl")
    }
  }

  private def withPageQuery(url: ActionDefinition, page: Int): String = {
    val urlStr = url.url
    if (urlStr.contains('?')) {
      urlStr + "page=" + page
    } else {
      urlStr + "?page=" + page
    }
  }
}