package models

import com.google.inject.Inject
import enums._
import java.sql.Timestamp
import java.util.Date
import org.joda.money.{CurrencyUnit, Money}
import org.joda.time.DateMidnight
import org.squeryl.Query
import services.{AppConfig, Time}
import services.db.{FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey}
import services.Finance.TypeConversions._
import util.Random

case class CouponServices @Inject()(store: CouponStore)

case class Coupon(
  id: Long = 0,
  name: String = "",
  code: String = Coupon.generateCode,
  startDate: Timestamp = new Timestamp(new DateMidnight().getMillis),
  endDate: Timestamp = new Timestamp(new DateMidnight().plusYears(10).getMillis),
  discountAmount: BigDecimal = 5,
  _couponType: String = CouponType.Promotion.name,
  _discountType: String = CouponDiscountType.Flat.name,
  _usageType: String = CouponUsageType.OneUse.name,
  isActive: Boolean = true,
  restrictions: String = "{}", // SER-508
  lineItemTypeId: Long = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CouponServices = AppConfig.instance[CouponServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated 
  with HasCouponType[Coupon]
  with HasCouponDiscountType[Coupon]
  with HasCouponUsageType[Coupon]
{
  
  /**
   * Marks this coupon as used. Does nothing if this coupon is unlimited-use.
   */
  def use(implicit amount: Money = Money.zero(CurrencyUnit.USD)): Coupon = {
    usageType match {
      case CouponUsageType.Unlimited => this
      case CouponUsageType.Prepaid if ((discountAmount - amount.getAmount) >= 0.01) =>
        copy(discountAmount = discountAmount - amount.getAmount)
      case _ => copy(isActive = false)
      // TODO: another case for prepaid to issue new coupon for remaining balance
    }
  }
  
  /**
   * @return the discount amount. If the discount amount would otherwise be greater than the preCouponAmount, then preCouponAmount is returned.
   */
  def calculateDiscount(preCouponAmount: Money): Money = {
    discountType match {
      case CouponDiscountType.Flat => {
        discountAmount.min(preCouponAmount.getAmount).toMoney(CurrencyUnit.USD)
      }
      case _ => {
        ((discountAmount / 100) * preCouponAmount.getAmount).toMoney(CurrencyUnit.USD)
      }
    }
  }
  
//  SER-508
//  /**
//   * @return the amount that should be invoiced to the corporate account.
//   * (to be implemented: corporate account)
//   */
//  def calculateInvoiceAmount(preCouponAmount: BigDecimal): BigDecimal = {
//    if (couponType == CouponType.Invoiceable) {
//      calculateDiscount(preCouponAmount)
//    } else {
//      0
//    }
//  }
  
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): Coupon = {
    discountType match {
      case CouponDiscountType.Flat => {
        require(discountAmount > 0, "For flat coupons, discount amount must be greater than 0.")
      }
      case CouponDiscountType.Percentage if (usageType == CouponUsageType.Unlimited) => {
        require(discountAmount > 0, "Discount percentage amount must be between 0 and 25 for unlimited use coupons.")
        require(discountAmount <= 25, "Discount percentage amount must be between 0 and 25 for unlimited use coupons.")
      }
      case _ => {
        require(discountAmount > 0, "Discount percentage amount must be between 0 and 100.")
        require(discountAmount <= 100, "Discount percentage amount must be between 0 and 100.")
      }
    }
    
    if (isActive) {
      services.store.findValid(code).foreach(existingCoupon =>
        require(existingCoupon.id == id, "A valid coupon with that code already exists.")
      )
    }
    
    services.store.save(this)
  }

  override def unapplied = Coupon.unapply(this)
  
  override def withCouponType(value: CouponType) = this.copy(_couponType = value.name)
  
  override def withDiscountType(value: CouponDiscountType) = this.copy(_discountType = value.name)
  
  override def withUsageType(value: CouponUsageType) = this.copy(_usageType = value.name)
}

object Coupon {
  protected[models] val defaultCodeLength = 12
  def generateCode: String = Random.alphanumeric.take(defaultCodeLength).mkString.toLowerCase
}

class CouponStore @Inject()(schema: Schema, 
    couponQueryFilters: CouponQueryFilters, 
    cashTransactionStore: CashTransactionStore) 
  extends SavesWithLongKey[Coupon] 
  with SavesCreatedUpdated[Coupon]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public methods
  //
  
  def findByFilter(filters: FilterOneTable[Coupon]*): Query[Coupon] = {
    from(schema.coupons)(coupon =>
      where(FilterOneTable.reduceFilters(filters, coupon))
      select(coupon)
      orderBy (coupon.id desc)
    )
  }
  
  def findByCode(code: String, filters: FilterOneTable[Coupon]*): Query[Coupon] = {
    from(schema.coupons)(coupon =>
      where(coupon.code === code.toLowerCase and FilterOneTable.reduceFilters(filters, coupon))
      select(coupon)
      orderBy (coupon.id desc)
    )
  }

  def findValid(code: String): Option[Coupon] = {
    findByCode(code, couponQueryFilters.activeByDate, couponQueryFilters.activeByFlag).headOption match {
      case None => None
      case Some(coupon) => {
        // TODO: apply restrictions
        Some(coupon)
      }
    }
  }

  def findByLineItemTypeId(id: Long): Query[Coupon] = {
    from(schema.coupons)(coupon =>
      where(coupon.lineItemTypeId === id)
      select(coupon)
      orderBy(coupon.updated desc)
    )
  }


  //
  // SavesWithLongKey[Coupon] methods
  //
  override val table = schema.coupons


  
  beforeInsertOrUpdate(withCodeInLowerCase)

  private def withCodeInLowerCase(toUpdate: Coupon): Coupon = {
    toUpdate.copy(code = toUpdate.code.trim.toLowerCase)
  }

  //
  // SavesCreatedUpdated[Coupon] methods
  //
  override def withCreatedUpdated(toUpdate: Coupon, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}

class CouponQueryFilters @Inject() (schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  def activeByFlag: FilterOneTable[Coupon] = {
    new FilterOneTable[Coupon] {
      override def test(coupon: Coupon) = {
        coupon.isActive === true
      }
    }
  }
  
  def activeByDate: FilterOneTable[Coupon] = {
    new FilterOneTable[Coupon] {
      override def test(coupon: Coupon) = {
        Time.now between(coupon.startDate, coupon.endDate)
      }
    }
  }
  
  def oneUse: FilterOneTable[Coupon] = {
    new FilterOneTable[Coupon] {
      override def test(coupon: Coupon) = {
        (coupon._usageType === CouponUsageType.OneUse.name)
      }
    }
  }
  
  def unlimited: FilterOneTable[Coupon] = {
    new FilterOneTable[Coupon] {
      override def test(coupon: Coupon) = {
        (coupon._usageType === CouponUsageType.Unlimited.name)
      }
    }
  }
}
