package models.checkout

import models.{CashTransactionStore, CashTransaction}
import models.enums.{CashTransactionType, CodeType, LineItemNature}
import org.joda.money.Money
import scalaz.{Scalaz, Lens}
import services.db.{InsertsAndUpdatesAsEntity, QueriesAsEntity, CanInsertAndUpdateAsThroughServices, Schema}
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import com.google.inject.Inject
import play.api.libs.json.Json


case class CashTransactionLineItemType (
  _entity: LineItemTypeEntity,
  accountId: Long,
  billingPostalCode: Option[String],
  stripeCardTokenId: Option[String],
  _maybeTransaction: Option[CashTransaction],
  services: CashTransactionLineItemTypeServices = AppConfig.instance[CashTransactionLineItemTypeServices]
) extends LineItemType[CashTransaction] with HasLineItemTypeEntity
  with LineItemTypeEntityLenses[CashTransactionLineItemType]
  with LineItemTypeEntityGetters[CashTransactionLineItemType]
{
  import checkout._

  // TODO(SER-499): more Json
  override def toJson = ""

  // lineItems makes line item for total from resolvedItems
  override def lineItems(resolved: LineItems, unresolved: LineItemTypes = Nil) = {
    _maybeTransaction match {

      case Some(txn: CashTransaction) => Some(Seq(CashTransactionLineItem(this, txn)))

      case None => resolved.find(item => item.codeType == CodeType.Total) match {
        case Some(total) =>
          require(total.amount.isZero || stripeCardTokenId.isDefined,
            "Stripe card token required for non-zero totals."
          )

          /** stripeChargeId set in Checkout transaction */
          val txn = CashTransaction(
            accountId = accountId,
            billingPostalCode = billingPostalCode,
            stripeCardTokenId = stripeCardTokenId
          ) .withCashTransactionType(CashTransactionType.Checkout)
            .withCash(total.amount.negated)


          // NOTE(SER-499): this shouldn't ever actually change anything if used correctly
          val txnType = if (total.amount.isNegative) {
            this.entity.set(CashTransactionLineItemType.refundEntity)
          } else {
            this.entity.set(CashTransactionLineItemType.paymentEntity)
          }

          Some(Seq(CashTransactionLineItem(txnType, txn)))

        case None => None
      }
    }
  }


  override protected lazy val entityLens = Lens[CashTransactionLineItemType, LineItemTypeEntity](
    get = txnType => txnType._entity,
    set = (txnType, newEntity) => txnType.copy(newEntity)
  )
}






object CashTransactionLineItemType {

  // TODO(SER-499): get entity from store or services instead since there's not really a need
  // for multiple cash transaction line item types
  def codeType = CodeType.CashTransaction
  def refundEntity = entityMap(LineItemNature.Refund)
  def paymentEntity = entityMap(LineItemNature.Payment)

  /**
   * This memoizes db calls to get the entities for payments and refund cash transaction line item types
   * NOTE(SER-499): think of a way to make this more testable
   */
  private val entityMap = Scalaz.immutableHashMapMemo {
    nature: LineItemNature => AppConfig.instance[CashTransactionLineItemTypeServices].findEntityByNature(nature)
  }

  //
  // Create
  //
  def apply(accountId: Long, billingPostalCode: Option[String], stripeCardTokenId: Option[String]) = {
    new CashTransactionLineItemType(
      _entity = paymentEntity,
      accountId = accountId,
      billingPostalCode = billingPostalCode,
      stripeCardTokenId = stripeCardTokenId,
      _maybeTransaction = None
    )
  }

  // TODO(SER-499): def payment and def refund, possibly replace use of apply

  //
  // Restore
  //
  def apply(entity: LineItemTypeEntity, cashTransaction: CashTransaction) = {
    // TODO(SER-499): remove entity if not needed
    new CashTransactionLineItemType(
      _entity = entity,
      accountId = cashTransaction.accountId,
      billingPostalCode = cashTransaction.billingPostalCode,
      stripeCardTokenId = cashTransaction.stripeCardTokenId,
      _maybeTransaction = Some(cashTransaction)
    )
  }


  // TODO(SER-499): manage entity in a reasonably performant way, considering that it may be a singleton
  //  type CashTxnLITServices = CashTransactionLineItemTypeServices
  //  protected def entity(
  //    implicit services: CashTxnLITServices = AppConfig.instance[CashTxnLITServices]
  //  ): LineItemTypeEntity = {
  //    LineItemTypeEntity(
  //      desc = CodeType.CashTransaction.name + " entity",
  //      nature = LineItemNature.Charge,
  //      codeType = CodeType.CashTransaction
  //    )
  //  }
}













case class CashTransactionLineItemTypeServices @Inject() (
  schema: Schema
) extends SavesAsLineItemTypeEntity[CashTransactionLineItemType] {
  import org.squeryl.PrimitiveTypeMode._

  override protected def modelWithNewEntity(
    txn: CashTransactionLineItemType,
    newEntity: LineItemTypeEntity
  ): CashTransactionLineItemType = { txn.entity.set(newEntity) }

  protected def codeType = CashTransactionLineItemType.codeType

  protected def entityDesc(nature: LineItemNature) = {
    "%s %s entity".format(codeType.name, nature.name)
  }

  protected def entityFromNature(nature: LineItemNature) = {
    LineItemTypeEntity(entityDesc(nature), nature, codeType)
  }

  def findEntityByNature(nature: LineItemNature) = {
    schema.lineItemTypes.where(entity =>
      entity._codeType === codeType.name and entity._nature === nature.name
    ).headOption.getOrElse(
      this.insert(entityFromNature(nature))
    )
  }


}





class CashTransactionLineItemTypeStore @Inject() (
  schema: Schema
) {


}