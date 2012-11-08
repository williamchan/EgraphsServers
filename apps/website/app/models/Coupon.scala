package models

import com.google.inject.Inject
import enums._
import java.sql.Timestamp
import java.util.Date
import org.joda.money.Money
import org.joda.time.DateTime
import org.squeryl.Query
import services.{AppConfig, Time}
import services.db.{FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey}
import util.Random

case class CouponServices @Inject()(store: CouponStore)

case class Coupon(id: Long = 0,
                  name: String = "",
                  code: String = Coupon.generateCode,
                  startDate: Date = Time.today,
                  endDate: Date = new DateTime().plusYears(10).toDate,
                  discountAmount: BigDecimal = 5,
                  _couponType: String = CouponType.Promotion.name,
                  _discountType: String = CouponDiscountType.Flat.name,
                  _usageType: String = CouponUsageType.OneUse.name,
                  isActive: Boolean = true,
//                  restrictions: String = "",
//                  corporateGroupId: Option[Long] = None,
                  created: Timestamp = Time.defaultTimestamp,
                  updated: Timestamp = Time.defaultTimestamp,
                  services: CouponServices = AppConfig.instance[CouponServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated 
  with HasCouponType[Coupon]
  with HasCouponDiscountType[Coupon]
  with HasCouponUsageType[Coupon]
  {
  
  /**
   * Marks this coupon as used. Does nothing if this coupon is unlimited-use.
   */
  def use(): Coupon = {
    usageType match {
      case CouponUsageType.Unlimited => this
      case _ => copy(isActive = false)
      // TODO: another case for prepaid to issue new coupon for remaining balance
    }
  }
  
  /**
   * @return the discount amount. If the discount amount would otherwise be greater than the preCouponAmount, then preCouponAmount is returned.
   */
  def calculateDiscount(preCouponAmount: BigDecimal): BigDecimal = {
    discountType match {
      case CouponDiscountType.Flat => {
        discountAmount.min(preCouponAmount)
      }
      case _ => {
        (discountAmount / 100) * preCouponAmount
      }
    }
  }
  
  /**
   * Returns true if usage of this coupon should result in an amount being invoiced to a corporate account.
   * (to be implemented: corporate account)
   */
  def shouldChargeRemainder: Boolean = {
    couponType != CouponType.Invoiceable
  }
  
  /**
   * @return the amount that should be invoiced to the corporate account.
   * (to be implemented: corporate account)
   */
  def calculateInvoiceAmount(preCouponAmount: BigDecimal): BigDecimal = {
    if (couponType == CouponType.Invoiceable) {
      calculateDiscount(preCouponAmount)
    } else {
      0
    }
  }
  
  //
  // Public methods
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): Coupon = {
    discountType match {
      case CouponDiscountType.Flat => {
        require(discountAmount > 0, "For flat coupons, discount amount must be greater than 0.")
      }
      case _ => {
        require(discountAmount > 0, "For percentage coupons, discount amount must be between 0 and 100.")
        require(discountAmount <= 100, "For percentage coupons, discount amount must be between 0 and 100.")
      }
    }
    
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Coupon.unapply(this)
  
  override def withCouponType(value: CouponType.EnumVal) = {
    this.copy(_couponType = value.name)
  }
  
  override def withDiscountType(value: CouponDiscountType.EnumVal) = {
    this.copy(_discountType = value.name)
  }
  
  override def withUsageType(value: CouponUsageType.EnumVal) = {
    this.copy(_usageType = value.name)
  }
}

object Coupon {
  protected[models] val defaultCodeLength = 12
  def generateCode: String = Random.alphanumeric.take(defaultCodeLength).mkString
}

class CouponStore @Inject()(schema: Schema, 
    couponQueryFilters: CouponQueryFilters, 
    cashTransactionStore: CashTransactionStore) 
  extends SavesWithLongKey[Coupon] 
  with SavesCreatedUpdated[Long,Coupon] 
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public methods
  //
  
  def findByCode(code: String, filters: FilterOneTable[Coupon]*): Seq[Coupon] = {
    from(schema.coupons)(coupon =>
      where(coupon.code === code and FilterOneTable.reduceFilters(filters, coupon))
      select(coupon)
    ).toSeq
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
  
  //
  // SavesWithLongKey[Coupon] methods
  //
  override val table = schema.coupons

  override def defineUpdate(theOld: Coupon, theNew: Coupon) = {
    updateIs(
      theOld.name := theNew.name,
      theOld.code := theNew.code,
      theOld.startDate := theNew.startDate,
      theOld.endDate := theNew.endDate,
      theOld.discountAmount := theNew.discountAmount,
      theOld._discountType := theNew._discountType,
      theOld._usageType := theNew._usageType,
      theOld._couponType := theNew._couponType,
      theOld.isActive := theNew.isActive,
//      theOld.restrictions := theNew.restrictions,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,Coupon] methods
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
        Time.today between(coupon.startDate, coupon.endDate)
      }
    }
  }
  
}
