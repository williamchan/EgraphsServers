package controllers

import play.api.mvc.Controller
import play.api.templates.Html
import java.util
import org.joda.money.{CurrencyUnit, Money}


/**
 * Permutations of the Checkout: Review.
 */
object Review extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{
  def index = {
    DefaultRenderer().render
  }

  def portrait = {
    DefaultRenderer(
      productPreviewUrl = "http://placehold.it/302x420",
      orientation="orientation-portrait"
    ).render
  }  

  def withPrint = {
    DefaultRenderer().copy(highQualityPrint=true).render
  }

  case class DefaultRenderer(
    celebrityName: String = "{celebrity name}",
    productName: String = "{product title}",
    celebrityWillWrite: String = "{what he will write}",
    recipientName: String = "{recipient name}",
    noteToCelebrity: Option[String] = Some("{note to celebrity}"),
    basePrice: org.joda.money.Money = Money.zero(CurrencyUnit.USD),
    guaranteedDelivery: java.util.Date = new util.Date(),
    highQualityPrintParamName: String = "highqualityprintparamname",
    highQualityPrint: Boolean = false,
    actionUrl: String = "/action-url",
    productPreviewUrl: String = "http://placehold.it/454x288",
    orientation:String = "orientation-landscape"
  ) {
    def render: Html = {
      views.html.frontend.celebrity_storefront_review(
        celebrityName,
        productName,
        celebrityWillWrite,
        recipientName,
        noteToCelebrity,
        basePrice,
        guaranteedDelivery,
        highQualityPrintParamName,
        highQualityPrint,
        actionUrl,
        productPreviewUrl,
        orientation
      )
    }
  }
}

