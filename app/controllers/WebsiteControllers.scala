package controllers

import browser._
import play.mvc.Controller
import services.http.{CelebrityAccountRequestFilters, DBTransaction}
import website.GetRootEndpoint
import services.{Mail, AppConfig}
import models.{AccountStore, CustomerStore, OrderStore}
import services.blobs.Blobs

object WebsiteControllers extends Controller
  with GetRootEndpoint
  with PostBuyProductEndpoint
  with GetCelebrityEndpoint
  with GetBlobEndpoint
  with GetEgraphEndpoint
  with GetCelebrityProductEndpoint
  with GetOrderConfirmationEndpoint
  with DBTransaction
{
  import AppConfig.instance

  override protected def orderStore = instance[OrderStore]
  override protected def celebFilters = instance[CelebrityAccountRequestFilters]
  override protected def blobs = instance[Blobs]
  override protected def mail = instance[Mail]
  override protected def customerStore = instance[CustomerStore]
  override protected def accountStore = instance[AccountStore]
}