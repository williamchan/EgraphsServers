package services.email

import models.Celebrity
import models.Coupon
import models.Order
import models.OrderServices
import models.enums.{CouponDiscountType, CouponUsageType}
import models.frontend.email.{EgraphSignedEmailViewModel, GiftEgraphSignedEmailViewModel}
import org.apache.commons.mail.{ Email, HtmlEmail }
import play.api.templates.Html
import controllers.website.GetEgraphEndpoint
import java.sql.Timestamp
import utils.TestData

/**
 * A home for utility functions that help prepare the "egraph signed" emails.
 * Useful for both the regular case and the gift-giving case.
 */
object EgraphSignedEmailPreparer {

  def prepareEgraphSignedEmailHelper(order: Order, services: OrderServices): (String, Celebrity, HtmlEmail, Coupon) = {

    val maybeCelebrity = services.celebrityStore.findByOrderId(order.id)
    maybeCelebrity match {
      case None => {
        play.Logger.error("There is no celebrity associated with order ID: " + order.id)
        throw new IllegalStateException("There is no celebrity associated with order ID: " + order.id)
      }
      case Some(celebrity) => {
        val email = new HtmlEmail()

        val buyingCustomer = order.buyer
        val receivingCustomer = order.recipient
        email.setFrom(celebrity.urlSlug + "@egraphs.com", celebrity.publicName)
        email.addTo(receivingCustomer.account.email)
        if (buyingCustomer != receivingCustomer) {
          email.addCc(buyingCustomer.account.email)
        }

        email.addReplyTo("webserver@egraphs.com")
        email.setSubject("I just finished signing your Egraph")

        val viewEgraphUrl = services.consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraphClassic(order.id).url)
        val coupon = getNewPercentOffCouponCode(15, TestData.sevenDaysHence.getTime)

        (viewEgraphUrl, celebrity, email, coupon)
      }
    }
  }

  // Creates a one-time coupon with the given percentage off any purchase made in the next week  
  private def getNewPercentOffCouponCode(percent: Int, timeToExpire: Long): Coupon = {
    if (percent < 0 || percent > 100)
      throw new IllegalArgumentException("The value percent must be between 0 and 100. Value given: " + percent)
    else
      Coupon(startDate = new Timestamp(TestData.today.getTime), endDate = new Timestamp(timeToExpire),
        discountAmount = percent, _discountType = CouponDiscountType.Percentage.name, _usageType = CouponUsageType.OneUse.name).save()
  }
    
  def getHtmlAndTextMsgs(egraphSignedEmailStack: EgraphSignedEmailViewModel): (Html, String) = {    
    val htmlMsg = views.html.frontend.email.view_egraph(egraphSignedEmailStack)    
    val textMsg = views.txt.frontend.email.view_egraph(egraphSignedEmailStack).toString
    (htmlMsg, textMsg)
  }

  def getGiftHtmlAndTextMsgs(egraphSignedEmailStack: GiftEgraphSignedEmailViewModel): (Html, String) = {
    val htmlMsg = views.html.frontend.email.view_gift_egraph(egraphSignedEmailStack)
    val textMsg = views.txt.frontend.email.view_gift_egraph(egraphSignedEmailStack).toString
    (htmlMsg, textMsg)
  }
}