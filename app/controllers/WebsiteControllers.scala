package controllers

import browser._
import play.mvc.Controller
import services.AppConfig
import models.OrderStore
import services.Blobs
import services.http.{CelebrityAccountRequestFilters, DBTransaction}
import website.GetRootEndpoint

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

  protected def orderStore = instance[OrderStore]
  protected def celebFilters = instance[CelebrityAccountRequestFilters]
  protected def blobs = instance[Blobs]
}