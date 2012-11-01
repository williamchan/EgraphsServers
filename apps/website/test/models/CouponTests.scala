package models

import enums._
import utils._
import services.AppConfig

class CouponTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[Coupon]
  with CreatedUpdatedEntityTests[Long, Coupon]
  with DateShouldMatchers
  with DBTransactionPerTest {

  private def store = AppConfig.instance[CouponStore]
  private def couponQueryFilters = AppConfig.instance[CouponQueryFilters]

  //
  // SavingEntityTests[Coupon] methods
  //
  override def newEntity = {
    Coupon(startDate = TestData.today, endDate = TestData.sevenDaysHence)
  }

  override def saveEntity(toSave: Coupon) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Coupon) = {
    toTransform.copy(
      name = "Egraphs Coupon")
  }

  //
  // Test cases
  //
  "coupon" should "start with a generated code" in {
    newEntity.code.length should be(Coupon.defaultCodeLength)
  }
  
  "save" should "throw exception if discountType is Flat but discountAmount is not greater than 0" in {
    val exception = intercept[IllegalArgumentException] {Coupon(discountAmount = 0).withDiscountType(CouponDiscountType.Flat).save()}
    exception.getLocalizedMessage should include("For flat coupons, discount amount must be greater than 0")
  }
  
  "save" should "throw exception if discountType is Percentage but discountAmount is outside 0-100 range" in {
    val exception0 = intercept[IllegalArgumentException] {Coupon(discountAmount = 0).withDiscountType(CouponDiscountType.Percentage).save()}
    exception0.getLocalizedMessage should include("For percentage coupons, discount amount must be between 0 and 100")
    
    val exception1 = intercept[IllegalArgumentException] {Coupon(discountAmount = 101).withDiscountType(CouponDiscountType.Percentage).save()}
    exception1.getLocalizedMessage should include("For percentage coupons, discount amount must be between 0 and 100")
  }
  
  "calculateDiscount" should "calculate flat discounts" in {
    val coupon10 = Coupon(discountAmount = 10).withDiscountType(CouponDiscountType.Flat).save()
    coupon10.calculateDiscount(50) should be(10)
    
    val coupon100 = Coupon(discountAmount = 100).withDiscountType(CouponDiscountType.Flat).save()
    coupon100.calculateDiscount(50) should be(50)
  }

  "calculateDiscount" should "calculate percentage discounts" in {
    val coupon10 = Coupon(discountAmount = 10).withDiscountType(CouponDiscountType.Percentage).save()
    coupon10.calculateDiscount(50) should be(5)
    
    val coupon100 = Coupon(discountAmount = 100).withDiscountType(CouponDiscountType.Percentage).save()
    coupon100.calculateDiscount(50) should be(50)
  }
  
  "calculateInvoiceAmount" should "return discount amount if coupon type is invoiceable" in (pending)

  "findByCode" should "filter by code" in {
    val coupon = newEntity.save()
    store.findByCode(coupon.code).toList should be(List(coupon))
  }
  
  "findByCode" should "filter by date when activeByDate is applied" in {
    val coupon = newEntity.save()
    val code = coupon.code
    /*expired coupon*/ Coupon(code = code, startDate = TestData.jan_01_2012, endDate = TestData.jan_08_2012).save()
    /*future coupon */ Coupon(code = code, startDate = TestData.tomorrow, endDate = TestData.twoDaysHence).save()
    store.findByCode(coupon.code, couponQueryFilters.activeByDate).toList should be(List(coupon))
  }
}
