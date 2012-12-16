package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import org.squeryl.KeyedEntity
import java.sql.Timestamp
import services.db.{SavesAsEntity, Schema}
import services.{MemberLens, Time}
import scalaz.Lens

trait LineItem[+TransactedT] extends HasLineItemEntity {
  def itemType: LineItemType[TransactedT]
  def amount: Money
  def subItems: Seq[LineItem[_]]  // NOTE: Seq > IndexedSeq bc of monadic use

  def checkoutId: Long
  def _domainEntityId: Long

  // new, 12/13/12
  def toJson: String // TODO(SER-499): Use Json type
  def domainObject: TransactedT


  /** @return flat sequence of this LineItem and its sub-LineItems */
  def flatten: IndexedSeq[LineItem[_]] = {
    val seqOfFlatSubItemSeqs = for(subItem <- subItems) yield subItem.flatten
    IndexedSeq(this) ++ seqOfFlatSubItemSeqs.flatten
  }

  /**
   * TODO(SER-499): should transact operate on flattened LineItems or not?
   * Persists line item and its fields (line item type, domain object, etc) as necessary.
   * Note that a summary line item might not choose to actually persist itself, so this
   * definition should allow such implementation.
   *
   * @param newCheckoutId - id of the checkout being transacted as a part of
   * @return persisted line item
   */
  def transact(newCheckoutId: Long): LineItem[TransactedT]
}

case class LineItemEntity(
  id: Long = 0L,
  _checkoutId: Long = 0L,
  _itemTypeId: Long = 0L,
  _amountInCurrency: BigDecimal = BigDecimal(0),
  notes: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedEntity[Long]




trait LineItemEntityLenses[T <: LineItem[_]] { this: T =>
  import MemberLens.Conversions._

  /**
   * Specifies how to get and set the entity into your object. For example:
   *
   * {{{
   *   import scalaz.Lens
   *   case class TaxLineItem(_entity: EntityLineItem)
   *     extends LineItemEntityLenses[TaxLineItem]
   *   {
   *     override protected lazy val entityLens = Lens[TaxLineItem, LineItemEntity](
   *       get = tax => tax._entity,
   *       set = (tax, newEntity) => tax.copy(_entity=entity)
   *     )
   *   }
   * }}}
   *
   */
  protected def entityLens: Lens[T, LineItemEntity]

  //
  // Private members
  //
  private def entity = entityLens.asMember(this)

  private[checkout] lazy val checkoutIdField =
    entityField(get = _._checkoutId)(set = id => entity().copy(_checkoutId=id))
  private[checkout] lazy val itemTypeIdField =
    entityField(get = _._itemTypeId)(set = id => entity().copy(_itemTypeId=id))
  private[checkout] lazy val amountInCurrencyField = entityField
    (get = Money.of(CurrencyUnit.USD, _._amountInCurrency))
    (set = (amount: Money) => entity().copy(
      _amountInCurrency=amount.withCurrencyUnit(CurrencyUnit.USD).getAmount)
    )


  private def entityField[PropT](get: LineItemEntity => PropT)(set: PropT => LineItemEntity)
  : MemberLens[T, PropT] =
  {
    entity.xmap(entity => get(entity))(newProp => set(newProp)).asMember(this)
  }
}


trait LineItemEntityGetters[T <: LineItem[_]] { this: T with LineItemEntityLenses[T] =>
  lazy val checkoutId = checkoutIdField()
  lazy val itemTypeIdField = itemTypeIdField()
  lazy val amountInCurrency = amountInCurrencyField()
}


trait LineItemEntitySetters[T <: LineItem[_]] { this: T with LineItemEntityLenses[T] =>
  lazy val withCheckoutId = checkoutIdField.set _
  lazy val withItemTypeId = itemTypeIdField.set _
  lazy val withAmountInCurrency = amountInCurrencyField.set _
}