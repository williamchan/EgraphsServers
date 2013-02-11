package models.checkout

import com.google.inject.Inject
import models.CashTransaction
import models.checkout.checkout.Conversions._
import models.enums.{CashTransactionType, CheckoutCodeType, LineItemNature}
import scalaz.{Scalaz, Lens}
import services.db.{HasTransientServices, Schema}
import services.AppConfig

/**
 * CashTransactionLineItemTypes are either used for making a cash transaction or represent existing
 * cash transactions. They can be payments or refunds, which just reflects the sign of the charged
 * amount.
 *
 * New/unpersisted instances need a billingPostalCode, and stripeCardTokenId if the charged amount is non-zero.
 * Restored or persisted instances only need the entity and item entity.
 *
 * The entities are singular based on their LineItemNature; for now, no need is seen for multiple
 * payment cash transaction type entities (same for refunds and forseeably for invoices).
 *
 * @param _entity new or persisted LineItemTypeEntity
 * @param billingPostalCode needed for CashTransaction, otherwise None
 * @param stripeCardTokenId needed for CashTransaction, otherwise None
 * @param _maybeItemEntity None or persisted LineItemEntity
 * @param _services
 */
case class CashTransactionLineItemType protected (
  _entity: LineItemTypeEntity,
  billingPostalCode: Option[String],
  stripeCardTokenId: Option[String],
  _maybeItemEntity: Option[LineItemEntity],
  @transient _services: CashTransactionLineItemTypeServices = AppConfig.instance[CashTransactionLineItemTypeServices]
)
  extends LineItemType[CashTransaction]
  with HasLineItemTypeEntity[CashTransactionLineItemType]
  with LineItemTypeEntityLenses[CashTransactionLineItemType]
  with LineItemTypeEntityGetters[CashTransactionLineItemType]
  with HasTransientServices[CashTransactionLineItemTypeServices]
{

  override def toJson = ""

  /**
   * @param resolved should have a BalanceLineItem in order to resolve
   * @param unresolved should have no BalanceLineItemType in order to resolve
   * @return None if conditions for resolution are not met
   */
  override def lineItems(resolved: LineItems, unresolved: LineItemTypes = Nil)
  : Option[Seq[CashTransactionLineItem]] = {

    (_maybeItemEntity, unresolved(CheckoutCodeType.Balance)) match {

      // persisted, just pass along entities
      case (Some(itemEntity: LineItemEntity), _)  =>
        Some(Seq(CashTransactionLineItem(itemEntity, _entity)))


      // new line item type, create new CashTransaction
      case (None, Nil) =>
        val balance = resolved(CheckoutCodeType.Balance).head

        require(balance.amount.isZero || stripeCardTokenId.isDefined,
          "Stripe card token required for non-zero balance."
        )

        // note: stripeChargeId set when charge is made in Checkout transaction
        val txn = CashTransaction(
          billingPostalCode = billingPostalCode,
          stripeCardTokenId = stripeCardTokenId
        ) .withCashTransactionType(CashTransactionType.Checkout)
          .withCash(balance.amount)


        // check for consistent use of semantics and sign of amounts
        if (balance.amount.isNegative) {
          require(nature == LineItemNature.Refund)
        } else {
          require(nature == LineItemNature.Payment)
        }

        Some(Seq(CashTransactionLineItem(this, txn)))


      // cannot resolve yet
      case (_, Seq(_)) => None
    }
  }


  override protected lazy val entityLens = Lens[CashTransactionLineItemType, LineItemTypeEntity](
    get = txnType => txnType._entity,
    set = (txnType, newEntity) => txnType.copy(newEntity)
  )

  override def withEntity(newEntity: LineItemTypeEntity) = entity.set(newEntity)
}






object CashTransactionLineItemType {

  def codeType = CheckoutCodeType.CashTransaction
  def refundEntity = entityMap(LineItemNature.Refund)
  def paymentEntity = entityMap(LineItemNature.Payment)

  /**
   * This memoizes db calls to get the entities for payments and refund cash transaction line item types.
   *
   * Could also forseeably be of an Invoice nature.
   *
   * Alternatively, could just get from db; just wanted to play with this idea for having a fixed
   * number of item type entities.
   */
  private val entityMap = Scalaz.immutableHashMapMemo { nature: LineItemNature =>
    AppConfig.instance[CashTransactionLineItemTypeServices].getEntityByNatureOrCreate(nature)
  }

  //
  // Create
  //
  def apply(stripeCardTokenId: Option[String], billingPostalCode: Option[String]) = {
    new CashTransactionLineItemType(
      _entity = paymentEntity,
      billingPostalCode = billingPostalCode,
      stripeCardTokenId = stripeCardTokenId,
      _maybeItemEntity = None
    )
  }

  // todo(refunds): define a method to create refund transactions

  //
  // Restore
  //
  def apply(entity: LineItemTypeEntity, itemEntity: LineItemEntity) = {
    new CashTransactionLineItemType(
      _entity = entity,
      billingPostalCode = None,
      stripeCardTokenId = None,
      _maybeItemEntity = Some(itemEntity)
    )
  }
}









case class CashTransactionLineItemTypeServices @Inject() (
  schema: Schema,
  lineItemStore: LineItemStore
) extends SavesAsLineItemTypeEntity[CashTransactionLineItemType] {
  import org.squeryl.PrimitiveTypeMode._

  def getEntityByNatureOrCreate(nature: LineItemNature) = {
    schema.lineItemTypes.where(entity =>
      entity._codeType === codeType.name and entity._nature === nature.name
    ).headOption.getOrElse(
      this.insert(entityFromNature(nature))
    )
  }

  //
  // Helpers
  //
  private def entityFromNature(nature: LineItemNature) = LineItemTypeEntity(entityDesc(nature), nature, codeType)
  private def entityDesc(nature: LineItemNature) = "%s %s entity".format(codeType.name, nature.name)
  private def codeType = CashTransactionLineItemType.codeType
}





class CashTransactionLineItemTypeStore @Inject() (
  schema: Schema
) {


}