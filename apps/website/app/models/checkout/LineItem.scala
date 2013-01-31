package models.checkout

import checkout.Conversions._
import com.google.inject.Inject
import java.sql.Timestamp
import org.joda.money.{CurrencyUnit, Money}
import models.enums._
import models.SavesCreatedUpdated
import scalaz.Lens
import services.db.{InsertsAndUpdatesAsEntity, HasEntity, Schema}
import services.MemberLens
import org.squeryl.Query

/**
 * Represents an actual item or data within a checkout. Has the responsibility of persisting itself,
 * its domain object, and line item type as necessary.
 *
 * Future considerations:
 * * `subItems` removed since it was unused, but could be utilized in the future here or in
 *   `LineItemType` for a more structured resolution scheme...
 * * Could some of these members be refactored into traits
 *
 * @tparam T type of domain object represented by this LineItem
 * @see
 */
trait LineItem[+T] extends HasLineItemNature with HasCodeType {

  //
  // LineItem Members
  //
  def id: Long
  def checkoutId: Long
  def amount: Money

  /** instance T represented by this LineItem */
  def domainObject: T

  /** `LineItemType` that resolved/produced this `LineItem` */
  def itemType: LineItemType[T]


  //
  // HasLineItemNature and HasCodeType members
  //
  override def codeType: CheckoutCodeType = itemType.codeType
  override def nature: LineItemNature = itemType.nature


  //
  // LineItem Methods
  //
  def toJson: String  // TODO(CE-16):
  def withAmount(newAmount: Money): LineItem[T]
  def withCheckoutId(newCheckoutId: Long): LineItem[T]

  /** Transforms and persists a T and potentially any objects it contains, as needed. */
  def transact(checkout: Checkout): LineItem[T]

  /** Rough approximation for equality between line items based on  */
  def equalsLineItem(that: LineItem[_]) = { that != null && this.unpacked == that.unpacked }
  protected def unpacked = (id, amount, itemType.id, codeType, nature, domainObject)

  /** Returns option of this if it has the desired code type, otherwise None. */
  protected[checkout] def asCodeTypeOption[LIT <: LineItemType[_], LI <: LineItem[_]](
    desiredCodeType: CodeTypeFactory[LIT, LI]
  ): Option[LI] = {
    if (codeType != desiredCodeType) None
    else Some(this.asInstanceOf[LI])  // cast to return as actual type, rather than LineItem[LI]
  }
}



/** Provides queries for getting `LineItem`s */
class LineItemStore @Inject() (schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  protected def table = schema.lineItems

  def getItemsByCheckoutId(id: Long): LineItems = {
    /** get entities and turn them into `LineItem`s through their `CheckoutCodeType`s */
    val items = for (
      (itemEntity, typeEntity) <- findEntityPairsByCheckoutId(id);
      codeType: CheckoutCodeType <- CheckoutCodeType(typeEntity._codeType)
    ) yield {
      codeType.itemInstance(itemEntity, typeEntity)
    }

    items.toSeq
  }


  def findEntityById(id: Long): Option[LineItemEntity] = {
    from(table)( entity => where(entity.id === id) select(entity) )
      .headOption
  }


  def findById(id: Long): Option[LineItem[Any]] = {
    val entityPair = findEntityPairById(id).headOption

    entityPair match {
      case None => None
      case Some((itemEntity, typeEntity)) => Some(
        typeEntity.codeType.itemInstance(itemEntity, typeEntity)
      )
    }
  }


  /** Specifying the CheckoutCodeType allows the item to be fetched as its actual type safely */
  def findByIdWithCodeType[ LIT <: LineItemType[_], LI <: LineItem[_] ](
    id: Long, codeType: CodeTypeFactory[LIT, LI]
  ): Option[LI] = {
    findById(id) flatMap (_.asCodeTypeOption(codeType))
  }


  //
  // Helper methods
  //
  protected type EntityPair = (LineItemEntity, LineItemTypeEntity)
  protected def findEntityPairsByCheckoutId(id: Long): Query[EntityPair] = {
    join( schema.lineItems, schema.lineItemTypes ) ( (li, lit) =>
      select(li, lit) on (li._itemTypeId === lit.id and li._checkoutId === id)
    )
  }
  protected def findEntityPairById(id: Long): Query[EntityPair] = {
    join( schema.lineItems, schema.lineItemTypes ) ( (li, lit) =>
      select(li, lit) on (li._itemTypeId === lit.id and li.id === id)
    )
  }

}





/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

trait HasLineItemEntity extends HasEntity[LineItemEntity, Long] { this: LineItem[_] => }

trait SavesAsLineItemEntity[ModelT <: HasLineItemEntity]
  extends InsertsAndUpdatesAsEntity[ModelT, LineItemEntity]
  with SavesCreatedUpdated[LineItemEntity]
{
  protected def schema: Schema
  override protected val table = schema.lineItems

  override protected def withCreatedUpdated(toUpdate: LineItemEntity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}





/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

trait LineItemEntityLenses[T <: LineItem[_]] { this: T with HasLineItemEntity =>
  import services.Finance.TypeConversions._
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
  def entity = entityLens.asMemberOf(this)

  //
  // Private members
  //
  private[checkout] lazy val checkoutIdField = entityField(
    get = _._checkoutId)(
    set = id => entity().copy(_checkoutId=id)
  )
  private[checkout] lazy val itemTypeIdField = entityField(
    get = _._itemTypeId)(
    set = id => entity().copy(_itemTypeId=id)
  )
  private[checkout] lazy val amountField = entityField(
    get = _._amountInCurrency.toMoney())(
    set = (amount: Money) => entity().copy(_amountInCurrency = amount.withCurrencyUnit(CurrencyUnit.USD).getAmount)
  )


  private def entityField[PropT](get: LineItemEntity => PropT)(set: PropT => LineItemEntity)
  : MemberLens[T, PropT] =
  {
    entity.xmap(entity => get(entity))(newProp => set(newProp)).asMemberOf(this)
  }
}


trait LineItemEntityGetters[T <: LineItem[_]] { this: T with LineItemEntityLenses[T] =>
  override lazy val checkoutId = checkoutIdField()
  override lazy val amount = amountField()
  lazy val itemTypeId = itemTypeIdField()
}


trait LineItemEntitySetters[T <: LineItem[_]] { this: T with LineItemEntityLenses[T] =>
  override def withCheckoutId(newId: Long) = checkoutIdField.set(newId)
  override def withAmount(newAmount: Money) = amountField.set(newAmount)
  lazy val withItemTypeId = itemTypeIdField.set _
}

trait LineItemEntityGettersAndSetters[T <: LineItem[_]]
  extends LineItemEntityLenses[T]
  with LineItemEntityGetters[T]
  with LineItemEntitySetters[T] { this: T with HasLineItemEntity => }
