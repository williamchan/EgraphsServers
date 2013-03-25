package services.mail

import models.Coupon
import models.enums.EmailType
import models.frontend.email._
import org.joda.time.DateTime
import services.coupon.CouponCreator

object MailUtils {

  def baseList(title: String) = List(("title", "<title>" + title + "</title>"))

  def getAccountVerificationTemplateContentParts(emailType: EmailType.EnumVal, accountVerificationEmailStack: AccountVerificationEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) ::: 
    List(("account_verification", views.html.frontend.tags.email.account_verification(accountVerificationEmailStack).body))
  }

  def getAccountConfirmationTemplateContentParts(emailType: EmailType.EnumVal): List[(String, String)] = {
    baseList(emailType.name) ::: 
    List(("account_confirmation", views.html.frontend.tags.email.account_confirmation().body))
  }

  def getCelebrityRequestTemplateContentParts(emailType: EmailType.EnumVal, celebrityRequestEmailStack: CelebrityRequestEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("celebrity_request", views.html.frontend.tags.email.celebrity_request(celebrityRequestEmailStack).body))
  }

  def getCelebrityWelcomeTemplateContentParts(emailType: EmailType.EnumVal, celebrityWelcomeEmailStack: CelebrityWelcomeEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("celebrity_welcome", views.html.frontend.tags.email.celebrity_welcome(celebrityWelcomeEmailStack).body))
  }

  def getEnrollmentCompleteTemplateContentParts(emailType: EmailType.EnumVal, enrollmentCompleteEmailStack: EnrollmentCompleteEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("enrollment_complete", views.html.frontend.tags.email.enrollment_complete(enrollmentCompleteEmailStack).body))
  }

  def getOrderConfirmationTemplateContentParts(emailType: EmailType.EnumVal, orderConfirmationEmailStack: OrderConfirmationEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("order_confirmation", views.html.frontend.tags.email.order_confirmation(orderConfirmationEmailStack).body))
  }

  def getResetPasswordTemplateContentParts(emailType: EmailType.EnumVal, resetPasswordEmailStack: ResetPasswordEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("reset_password", views.html.frontend.tags.email.reset_password(resetPasswordEmailStack).body))
  }

  def getViewEgraphTemplateContentParts(emailType: EmailType.EnumVal, viewEgraphEmailStack: ViewEgraphEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("view_egraph", views.html.frontend.tags.email.view_egraph(viewEgraphEmailStack).body),
         ("coupon", views.html.frontend.tags.email.promotional.coupon(getCouponViewModel(15, 7)).body))
  }

  def getViewGiftReceivedEgraphTemplateContentParts(emailType: EmailType.EnumVal, viewEgraphEmailStack: ViewEgraphEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("gift_received_header", views.html.frontend.tags.email.gift_received_header(viewEgraphEmailStack).body),
         ("view_egraph", views.html.frontend.tags.email.view_egraph(viewEgraphEmailStack).body),
         ("coupon", views.html.frontend.tags.email.promotional.coupon(getCouponViewModel(15, 7)).body))
  }

  def getViewGiftGivenEgraphTemplateContentParts(emailType: EmailType.EnumVal, viewEgraphEmailStack: ViewEgraphEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("gift_given_header", views.html.frontend.tags.email.gift_given_header(viewEgraphEmailStack).body),
         ("view_egraph", views.html.frontend.tags.email.view_egraph(viewEgraphEmailStack).body),
         ("coupon", views.html.frontend.tags.email.promotional.coupon(getCouponViewModel(15, 7)).body))
  }

  // Gets a % off coupon, with given percent off and given number of days to expiration
  private def getCouponViewModel(percentOff: Int, daysUntilExpiration: Int): CouponModuleEmailViewModel = {
    val someDaysHence = new DateTime().plusDays(daysUntilExpiration).toLocalDate.toDate
    val coupon = CouponCreator.getNewPercentOffCoupon(percentOff, someDaysHence.getTime)

    CouponModuleEmailViewModel(
      discountAmount = percentOff,
      code = coupon.code,
      daysUntilExpiration = daysUntilExpiration
    )
  }
}