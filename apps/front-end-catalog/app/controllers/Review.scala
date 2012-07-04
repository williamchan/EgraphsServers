package controllers

import play.mvc.Controller
import play.templates.Html
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
    actionUrl: String = "/action-url"
  ) {
    def render: Html = {
      views.frontend.html.celebrity_storefront_review(
        celebrityName,
        productName,
        celebrityWillWrite,
        recipientName,
        noteToCelebrity,
        basePrice,
        guaranteedDelivery,
        highQualityPrintParamName,
        highQualityPrint,
        actionUrl
      )
    }
  }
}

