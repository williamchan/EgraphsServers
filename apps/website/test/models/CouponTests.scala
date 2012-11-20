package models

import enums.CouponType._
import enums.CouponDiscountType._
import enums.CouponUsageType._
import java.sql.Timestamp
import org.joda.money.{CurrencyUnit, Money}
import utils._
import services.AppConfig
import services.Finance.TypeConversions._

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
    Coupon(startDate = new Timestamp(TestData.today.getTime), endDate = new Timestamp(TestData.sevenDaysHence.getTime))
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
  
  "save" should "save lower-cased code" in {
    val code = Coupon.generateCode
    Coupon(code = code.toUpperCase).save().code should be(code.toLowerCase)
  }
  
  "save" should "throw exception if a valid coupon with the same code already exists" in {
    val code = Coupon().save().code
    val exception = intercept[IllegalArgumentException] {Coupon(code = code).save()}
    exception.getLocalizedMessage should include("A valid coupon with that code already exists")
  }
  
  "save" should "throw exception if discountType is Flat but discountAmount is not greater than 0" in {
    val exception = intercept[IllegalArgumentException] {Coupon(discountAmount = 0).withDiscountType(Flat).save()}
    exception.getLocalizedMessage should include("For flat coupons, discount amount must be greater than 0")
  }
  
  "save" should "throw exception if coupon is one-use and percentage but discountAmount is outside 0-100 range" in {
    val exception0 = intercept[IllegalArgumentException] {Coupon(discountAmount = 0).withDiscountType(Percentage).withUsageType(OneUse).save()}
    exception0.getLocalizedMessage should include("Discount percentage amount must be between 0 and 100")
    
    val exception1 = intercept[IllegalArgumentException] {Coupon(discountAmount = 101).withDiscountType(Percentage).withUsageType(OneUse).save()}
    exception1.getLocalizedMessage should include("Discount percentage amount must be between 0 and 100")
    
    Coupon(discountAmount = 100).withDiscountType(Percentage).withUsageType(OneUse).save()
  }
  
  "save" should "throw exception if coupon is unlimited and percentage but discountAmount is outside 0-25 range" in {
    val exception0 = intercept[IllegalArgumentException] {Coupon(discountAmount = 0).withDiscountType(Percentage).withUsageType(Unlimited).save()}
    exception0.getLocalizedMessage should include("Discount percentage amount must be between 0 and 25 for unlimited use coupons")
    
    val exception1 = intercept[IllegalArgumentException] {Coupon(discountAmount = 26).withDiscountType(Percentage).withUsageType(Unlimited).save()}
    exception1.getLocalizedMessage should include("Discount percentage amount must be between 0 and 25 for unlimited use coupons")
    
    Coupon(discountAmount = 25).withDiscountType(Percentage).withUsageType(Unlimited).save()
  }
  
  "use" should "set isActive to false for one-use coupons" in {
    Coupon().withUsageType(OneUse).use.isActive should be(false)
    Coupon().withUsageType(Unlimited).use.isActive should be(true)
  }
  
  "calculateDiscount" should "calculate flat discounts" in {
    val tenDollars = BigDecimal(10).toMoney(CurrencyUnit.USD)
    val fiftyDollars = BigDecimal(50).toMoney(CurrencyUnit.USD)
    
    val coupon10 = Coupon(discountAmount = 10).withDiscountType(Flat).save()
    coupon10.calculateDiscount(fiftyDollars) should be(tenDollars)
    
    val coupon100 = Coupon(discountAmount = 100).withDiscountType(Flat).save()
    coupon100.calculateDiscount(fiftyDollars) should be(fiftyDollars)
  }

  "calculateDiscount" should "calculate percentage discounts" in {
    val fiveDollars = BigDecimal(5).toMoney(CurrencyUnit.USD)
    val fiftyDollars = BigDecimal(50).toMoney(CurrencyUnit.USD)
    
    val coupon10 = Coupon(discountAmount = 10).withDiscountType(Percentage).save()
    coupon10.calculateDiscount(fiftyDollars) should be(fiveDollars)
    
    val coupon100 = Coupon(discountAmount = 100).withDiscountType(Percentage).save()
    coupon100.calculateDiscount(fiftyDollars) should be(fiftyDollars)
  }
  
  "calculateInvoiceAmount" should "return discount amount if coupon type is invoiceable" in (pending)
  
  "findByCode" should "filter by code case-insensitively" in {
    val coupon = newEntity.save()
    store.findByCode(coupon.code.toUpperCase).toList should be(List(coupon))
  }
  
  "findByCode" should "filter by date when activeByDate is applied" in {
    val coupon = newEntity.save()
    val code = coupon.code
    /*expired coupon*/ Coupon(code = code, startDate = new Timestamp(TestData.jan_01_2012.getTime), endDate = new Timestamp(TestData.jan_08_2012.getTime), isActive = false).save()
    /*future coupon */ Coupon(code = code, startDate = new Timestamp(TestData.tomorrow.getTime), endDate = new Timestamp(TestData.twoDaysHence.getTime), isActive = false).save()
    store.findByCode(coupon.code, couponQueryFilters.activeByDate).toList should be(List(coupon))
  }
  
  "findValid" should "return a coupon matching code that are active by date and flag" in {
    val coupon = newEntity.save()
    val code = "mycode"
    /*inactive coupon*/ Coupon(code = code, isActive = false).save()
    /*expired coupon*/ Coupon(code = code, startDate = new Timestamp(TestData.jan_01_2012.getTime), endDate = new Timestamp(TestData.jan_08_2012.getTime)).save()
    /*future coupon */ Coupon(code = code, startDate = new Timestamp(TestData.tomorrow.getTime), endDate = new Timestamp(TestData.twoDaysHence.getTime)).save()
    store.findValid(coupon.code) should be(Some(coupon))
    
    coupon.copy(isActive = false).save()
    store.findValid(coupon.code) should be(None)
  }
}
