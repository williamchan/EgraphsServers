package controllers

import play.api._
import play.api.mvc._
import models.frontend.storefront.OrderCompleteViewModel
import java.util
import helpers.DefaultImplicitTemplateParameters
import egraphs.playutils.Gender

/**
 * Permutations of the Checkout: Order Complete.
 */
object Complete extends Controller with DefaultImplicitTemplateParameters {
  def index = Action {
    Ok(views.html.frontend.celebrity_storefront_complete(defaultOrderCompleteViewModel))
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
      celebrityGender = Gender.Male,
      productName="{product name}",
      productId=1L,
      totalPrice=BigDecimal(90.00).toMoney(),
      discount=Option(BigDecimal(10.00).toMoney()),
      digitalPrice=BigDecimal(70.00).toMoney(),
      printPrice=BigDecimal(30.00).toMoney(),
      expectedDeliveryDate=new util.Date(),
      faqHowLongLink = "https://www.egraphs.com/faq#how-long",
      printOrderShippingAddress = "1234 Cherry Lane New York, NY 12345",
      hasPrintOrder = true,
      withAffiliateMarketing = false
    )
  }

}

