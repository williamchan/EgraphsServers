package controllers

import play.mvc.Controller
import org.joda.money.Money
import models.frontend.storefront._
import scala.Some
import models.frontend.storefront.FinalizeBillingViewModel
import models.frontend.storefront.FinalizePriceViewModel
import scala.Some
import models.frontend.storefront.FinalizePersonalizationViewModel


/**
 * Permutations of the Checkout: Finalize.
 */
object Finalize extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{

  def index = {
    views.frontend.html.celebrity_storefront_finalize(defaultFinalizeViewModel)
  }
  
  def defaultShippingModel = {
    FinalizeShippingViewModel(
      name="{shipping name}",
      email="{shipping email}",
      addressLine1="{address 1}",
      addressLine2=Some("{address 2}"),
      city="{shipping city}",
      state="{shipping state}",
      postalCode="{postal code}",
      editUrl="{edit shipping url}"
    )
  }
  
  def defaultBillingModel = {
    FinalizeBillingViewModel (
      name="{billing name}",
      email="{email}",
      paymentToken="{paymentToken}",
      postalCode="{billing postal code}",
      paymentJsModule="{paymentJsModule}",
      editUrl="{edit billing url}"
    )
  }
  
  def defaultPersonalizationModel = {
    FinalizePersonalizationViewModel (
      celebName="{personalization celeb}",
      productTitle="{product title}",
      recipientName="{recipient name}",
      messageOption=PersonalizeMessageOption.SpecificMessage,
      messageText= "{message text}",
      editUrl="{edit personalization url"
    )
  }
  
  def defaultPriceModel = {
    import frontend.formatting.MoneyFormatting.Conversions._
        
    FinalizePriceViewModel (
      base= BigDecimal(1.11).toMoney(),
      physicalGood= Some(BigDecimal(2.22).toMoney()),
      tax= Some(BigDecimal(3.33).toMoney()),
      total= BigDecimal(4.00).toMoney()
    )
  }
  
  def defaultFinalizeViewModel = {
    FinalizeViewModel(
      billing=defaultBillingModel,
      shipping=Some(defaultShippingModel),
      personalization=defaultPersonalizationModel,
      price=defaultPriceModel,
      purchaseUrl="{purchase url}"
    )
  }

}

