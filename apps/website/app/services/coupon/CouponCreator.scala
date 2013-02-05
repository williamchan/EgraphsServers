package services.coupon

import models.Coupon
import java.sql.Timestamp
import utils.TestData
import models.enums.{CouponDiscountType, CouponUsageType}

object CouponCreator {
  
  // Creates a one-time coupon with the given percentage off any purchase made in before timeToExpire
  def getNewPercentOffCoupon(percent: Int, timeToExpire: Long): Coupon = {
    if (percent < 0 || percent > 100)
      throw new IllegalArgumentException("The value percent must be between 0 and 100. Value given: " + percent)
    else
      getNewCoupon(new Timestamp(TestData.today.getTime), new Timestamp(timeToExpire), percent, 
          CouponDiscountType.Percentage.name, CouponUsageType.OneUse.name)
  }
  
  def getNewCoupon(startDate: Timestamp, endDate: Timestamp, discountAmount: Long, discountType: String, 
      usageType: String): Coupon = {
    
    Coupon(startDate = startDate, endDate = endDate, discountAmount = discountAmount, _discountType = discountType, 
        _usageType = usageType).save()    
  }
}