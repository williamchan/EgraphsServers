package controllers

import play.mvc.Controller
import models.frontend.storefront.OrderCompleteViewModel
import java.util

/**
 * Permutations of the Checkout: Order Complete.
 */
object Complete extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{
  def index = {
    views.html.frontend.celebrity_storefront_complete(defaultOrderCompleteViewModel)
  }

  def defaultOrderCompleteViewModel = {
    import frontend.formatting.MoneyFormatting.Conversions._

    OrderCompleteViewModel(
      orderDate=new util.Date(),
      orderNumber=100L,
      buyerName="{buyer name}",
      buyerEmail="{buyer email}",
      ownerName="{owner name}",
      ownerEmail="{owner email}",
      celebName="{celebrity name}",
      productName="{product name}",
      totalPrice=BigDecimal(100.00).toMoney(),
      expectedDeliveryDate=new util.Date(),
      faqHowLongLink = "https://www.egraphs.com/faq#how-long",
      hasPrintOrder = true
    )
  }

}

