package controllers

import browser._
import nonproduction.PostBuyDemoProductEndpoint
import play.mvc.Controller
import services.http.{CelebrityAccountRequestFilters, DBTransaction}
import website.GetRootEndpoint
import models.{AccountStore, CustomerStore, OrderStore}
import services.blobs.Blobs
import services.{Payment, Mail, AppConfig}

object WebsiteControllers extends Controller
  with GetRootEndpoint
  with PostBuyProductEndpoint
  with PostBuyDemoProductEndpoint
  with GetCelebrityEndpoint
  with GetBlobEndpoint
  with GetEgraphEndpoint
  with GetCelebrityProductEndpoint
  with GetOrderConfirmationEndpoint
  with DBTransaction
{
  import AppConfig.instance

  override protected val payment = instance[Payment]
  override protected val orderStore = instance[OrderStore]
  override protected val celebFilters = instance[CelebrityAccountRequestFilters]
  override protected val blobs = instance[Blobs]
  override protected val mail = instance[Mail]
  override protected val customerStore = instance[CustomerStore]
  override protected val accountStore = instance[AccountStore]
}