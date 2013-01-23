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

trait LineItem[+TransactedT] extends Transactable[LineItem[TransactedT]] with HasLineItemNature with HasCodeType {

  def id: Long
  def itemType: LineItemType[TransactedT]
  def subItems: LineItems
  def toJson: String                    // TODO(SER-499): Use Json type, maybe even Option
  def domainObject: TransactedT


  /** @return flat sequence of this LineItem and its sub-LineItems */
  def flatten: LineItems = {
    val seqOfFlatSubItemSeqs = for(subItem <- subItems) yield subItem.flatten
    Seq(this) ++ seqOfFlatSubItemSeqs.flatten
  }

  //
  // Entity member accessors and mutators
  //
  def amount: Money
  def checkoutId: Long
  def withAmount(newAmount: Money): LineItem[TransactedT]
  def withCheckoutId(newCheckoutId: Long): LineItem[TransactedT]

  //
  // HasLineItemNature and HasCodeType members
  //
  override def codeType: CodeType = itemType.codeType
  override def nature: LineItemNature = itemType.nature


  /**
   * Rough approximation for equality between line items; does not detect difference in
   * implementation specific state.
   */
  def equalsLineItem(that: LineItem[_]): Boolean = {
    if (that != null) {
      def unpack(item: LineItem[_]) = (item.id, item.amount, item.itemType.id, item.codeType, item.nature, item.domainObject)

      unpack(this) == unpack(that)

    } else {
      false
    }
  }

  /** Returns option of this if it has the desired code type, otherwise None */
  protected[checkout] def asCodeTypeOption[LIT <: LineItemType[_], LI <: LineItem[_]](
    desiredCodeType: CodeTypeFactory[LIT, LI]
  ): Option[LI] = {
    if (codeType != desiredCodeType) None
    else Some(this.asInstanceOf[LI])  // cast to return as actual type, rather than LineItem[LI]
  }

}


/**
 * Provide helper queries for getting LineItem's; persistence is provided by LineItem implementations.
 */
class LineItemStore @Inject() (schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  protected def table = schema.lineItems

  // TODO(SER-499): helper queries
  // use CodeType of LineItemType to create LineItem's of the correct type

  def getItemsByCheckoutId(id: Long): LineItems = {
    /**
     * Note that using toSeq instead of toList returns a Stream which causes bugs in other steps
     * of restoring a checkout
     */
    join( schema.lineItems, schema.lineItemTypes ) ( (li, lit) =>
      select(li, lit) on (li._itemTypeId === lit.id and li._checkoutId === id)
    ).toList.flatMap { case (itemEntity, itemTypeEntity) =>
      CodeType(itemTypeEntity._codeType) match {
        case Some(codeType: CodeTypeFactory[_, _]) => Some(codeType.itemInstance(itemEntity, itemTypeEntity))
        case Some(_) => None // TODO(SER-499): add logging
        case None => None
      }
    }
  }

  def findEntityById(id: Long): Option[LineItemEntity] = {
    from(table)( entity =>
      where(entity.id === id) select(entity)
    ).headOption
  }

  def findByIdOfCodeType[ LIT <: LineItemType[_], LI <: LineItem[_] ](
    id: Long, codeType: CodeTypeFactory[LIT, LI]
  ): Option[LI] = {

    val maybeEntities = join( schema.lineItems, schema.lineItemTypes ) ( (li, lit) =>
      select(li, lit) on (li._itemTypeId === lit.id and li.id === id)
    ).headOption

    // If the entities are of the right code type, create line item and return it, otherwise None
    maybeEntities match {
      case Some((itemEntity, typeEntity)) if (CodeType(typeEntity._codeType) == Some(codeType)) =>
        Some(
          codeType.itemInstance(itemEntity, typeEntity)
        )
      case None => None
    }
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
