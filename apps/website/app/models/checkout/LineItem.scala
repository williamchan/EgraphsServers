package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import org.squeryl.KeyedEntity
import java.sql.Timestamp
import services.db.{SavesAsEntity, Schema}
import services.{MemberLens, Time}
import scalaz.Lens

trait LineItem[+TransactedT] extends HasLineItemEntity {
  def id = _entity.id
  def amount: Money = Money.of(CurrencyUnit.USD, _entity._amountInCurrency.bigDecimal)
  def itemType: LineItemType[TransactedT]
  def subItems: Seq[LineItem[_]]
  def toJson: String                    // TODO(SER-499): Use Json type
  def domainObject: TransactedT
  def _domainEntityId: Long

  /**
   * TODO(SER-499): should transact operate on flattened LineItems or not?
   * Persists line item and its fields (line item type, domain object, etc) as necessary.
   * Requires that checkoutId is set.
   *
   * Note that a summary line item might not choose to actually persist itself, so this
   * definition should allow such implementation.
   *
   * @return persisted line item
   */
  def transact: LineItem[TransactedT]

  /** @return flat sequence of this LineItem and its sub-LineItems */
  def flatten: IndexedSeq[LineItem[_]] = {
    val seqOfFlatSubItemSeqs = for(subItem <- subItems) yield subItem.flatten
    IndexedSeq(this) ++ seqOfFlatSubItemSeqs.flatten
  }

  def withCheckoutId(newCheckoutId: Long): LineItem[TransactedT]
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
  private[checkout] lazy val amountInCurrencyField = entityField(
    get = (entity: LineItemEntity) =>
      Money.of(CurrencyUnit.USD, entity._amountInCurrency.bigDecimal))(
    set = (amount: Money) => entity().copy(
      _amountInCurrency = amount.withCurrencyUnit(CurrencyUnit.USD).getAmount)
    )


  private def entityField[PropT](get: LineItemEntity => PropT)(set: PropT => LineItemEntity)
  : MemberLens[T, PropT] =
  {
    entity.xmap(entity => get(entity))(newProp => set(newProp)).asMember(this)
  }
}


trait LineItemEntityGetters[T <: LineItem[_]] { this: T with LineItemEntityLenses[T] =>
  lazy val checkoutId = checkoutIdField()
  lazy val itemTypeId = itemTypeIdField()
  lazy val amountInCurrency = amountInCurrencyField()
}


trait LineItemEntitySetters[T <: LineItem[_]] { this: T with LineItemEntityLenses[T] =>
  lazy val withCheckoutId = checkoutIdField.set _
  lazy val withItemTypeId = itemTypeIdField.set _
  lazy val withAmountInCurrency = amountInCurrencyField.set _
}