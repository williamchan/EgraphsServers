package controllers

import website._
import admin.{GetCreateCelebrityProductEndpoint, PostCelebrityProductEndpoint, PostCelebrityEndpoint, GetCreateCelebrityEndpoint}
import nonproduction.PostBuyDemoProductEndpoint
import website.GetRootEndpoint
import services.blobs.Blobs
import services.mail.Mail
import services.payment.Payment
import play.mvc.{Router, Controller}
import play.mvc.results.Redirect
import models._
import services.http.{AdminRequestFilters, CelebrityAccountRequestFilters, DBTransaction}

object WebsiteControllers extends Controller
  with GetRootEndpoint
  with PostBuyProductEndpoint
  with PostBuyDemoProductEndpoint
  with GetCelebrityEndpoint
  with GetBlobEndpoint
  with GetEgraphEndpoint
  with GetCelebrityProductEndpoint
  with GetOrderConfirmationEndpoint
  with GetCreateCelebrityEndpoint
  with PostCelebrityEndpoint
  with GetCreateCelebrityProductEndpoint
  with PostCelebrityProductEndpoint
  with DBTransaction {

  import services.AppConfig.instance

  // Provide endpoint dependencies
  override protected val adminFilters = instance[AdminRequestFilters]
  override protected val celebFilters = instance[CelebrityAccountRequestFilters]

  override protected val blobs = instance[Blobs]
  override protected val mail = instance[Mail]
  override protected val payment = instance[Payment]

  override protected val accountStore = instance[AccountStore]
  override protected val celebrityStore = instance[CelebrityStore]
  override protected val customerStore = instance[CustomerStore]
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
}