package controllers

import website._
import website.admin._
import example.{GetFacebookLoginEndpoint, GetSocialPostEndpoint}
import nonproduction.PostBuyDemoProductEndpoint
import services.blobs.Blobs
import services.mail.Mail
import services.payment.Payment
import play.mvc.{Router, Controller}
import models._
import play.mvc.Router.ActionDefinition
import play.mvc.results.Redirect
import services.db.DBSession
import java.util.Properties
import services.Utils
import services.http.{PlayConfig, SecurityRequestFilters, ControllerMethod, AdminRequestFilters, CelebrityAccountRequestFilters}
import services.social.FacebookAppId

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

  // customer account endpoints
  with GetRegisterEndpoint
  with PostRegisterEndpoint
  with GetLoginEndpoint
  with PostLoginEndpoint
  with PostLogoutEndpoint
  with GetRecoverAccountEndpoint
  with PostRecoverAccountEndpoint
  with GetRecoverAccountConfirmationEndpoint
  with GetResetPasswordEndpoint
  with PostResetPasswordEndpoint
  with PostFacebookLoginCallbackEndpoint

  // social media exploratory work
  with GetSocialPostEndpoint
  with GetFacebookLoginEndpoint
{

  import services.AppConfig.instance
  import services.AppConfig.annotatedInstance

  val adminIdKey: String = "admin"
  val customerIdKey: String = "customer"

  // Provide endpoint dependencies
  override protected val playConfig = annotatedInstance[PlayConfig, Properties]
  override protected val facebookAppId = annotatedInstance[FacebookAppId, String]

  override protected val dbSession = instance[DBSession]
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

  /**
   * Redirects with validation errors populated to flash scope.
   *
   * @param redirectUrl target URL
   * @param permanent See http://www.playframework.org/documentation/api/1.2.4/play/mvc/Controller.html#redirect(java.lang.String, boolean)
   * @return a redirect
   */
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

  /**
   * Updates the flash scope with pagination data used by pagination.scala.html
   */
  def updateFlashScopeWithPagingData[A](pagedQuery: (Iterable[A], Int, Option[Int]),
                                        baseUrl: ActionDefinition,
                                        filter: Option[String] = None) {
    val curPage = pagedQuery._2
    val totalResults = pagedQuery._3

    val showPaging = totalResults.isDefined && totalResults.get > Utils.defaultPageLength
    flash.put("ShowPaging", showPaging)
    val totalResultsStr = if (totalResults.isDefined) ("- " + totalResults.get + " results") else ""
    flash.put("TotalResultsStr", totalResultsStr)

    if (showPaging) {
      val showFirst: Boolean = curPage > 2
      flash.put("ShowFirst", showFirst)
      if (showFirst) flash.put("FirstUrl", withPageQuery(baseUrl, 1, filter)) else flash.remove("FirstUrl")

      val showPrev: Boolean = curPage > 1
      flash.put("ShowPrev", showPrev)
      if (showPrev) flash.put("PrevUrl", withPageQuery(baseUrl, curPage - 1, filter)) else flash.remove("PrevUrl")

      val totalNumPages = if (totalResults.get % Utils.defaultPageLength > 0) {
        totalResults.get / Utils.defaultPageLength + 1
      } else {
        totalResults.get / Utils.defaultPageLength
      }

      val showNext: Boolean = curPage < totalNumPages
      flash.put("ShowNext", showNext)
      if (showNext) flash.put("NextUrl", withPageQuery(baseUrl, curPage + 1, filter)) else flash.remove("NextUrl")

      val showLast: Boolean = curPage < totalNumPages
      flash.put("ShowLast", showLast)
      if (showLast) flash.put("LastUrl", withPageQuery(baseUrl, totalNumPages, filter)) else flash.remove("LastUrl")
    }
  }

  /**
   * Appends URL with query parameters
   *
   * @param url the URL to which to append query parameters
   * @param page the page index (as in pagination)
   * @param filter the "filter" parameter
   * @return url with query parameters appended
   */
  private def withPageQuery(url: ActionDefinition,
                            page: Int,
                            filter: Option[String]): String = {
    val urlStr = url.url

    val filterStr = filter match {
      case Some(f) => "&filter=" + f
      case None => ""
    }

    if (urlStr.contains('?')) {
      urlStr + "page=" + page + filterStr
    } else {
      urlStr + "?page=" + page + filterStr
    }
  }
}