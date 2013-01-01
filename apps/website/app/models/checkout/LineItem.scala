package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import org.squeryl.KeyedEntity
import java.sql.Timestamp
import services.db.{KeyedCaseClass, InsertsAndUpdatesAsEntity, HasEntity, Schema}
import services.{MemberLens, Time}
import scalaz.Lens
import models.{HasCreatedUpdated, SavesCreatedUpdated}
import org.squeryl.annotations.Transient
import com.google.inject.Inject
import models.enums.{CodeTypeFactory, LineItemNature, CodeType}

trait LineItem[+TransactedT] extends Transactable[LineItem[TransactedT]] {
  def id: Long

  def amount: Money
  def withAmount(newAmount: Money): LineItem[TransactedT]

  def checkoutId: Long
  def withCheckoutId(newCheckoutId: Long): LineItem[TransactedT]

  // def _typeEntity: LineItemTypeEntity
  // def _maybeType: Option[LineItemType[TransactedT]]
  def itemType: LineItemType[TransactedT]
  def subItems: Seq[LineItem[_]]
  def toJson: String                    // TODO(SER-499): Use Json type, maybe even Option
  def domainObject: TransactedT

  //def _domainEntityId: Long // getting rid of this in favor of taking domain object as argument



  /** @return flat sequence of this LineItem and its sub-LineItems */
  def flatten: IndexedSeq[LineItem[_]] = {
    val seqOfFlatSubItemSeqs = for(subItem <- subItems) yield subItem.flatten
    IndexedSeq(this) ++ seqOfFlatSubItemSeqs.flatten
  }

  // Convenience LineItemType member accessors
  def codeType: CodeType = itemType.codeType
  def nature: LineItemNature = itemType.nature
}


/**
 * Provide helper queries for getting LineItem's; persistence is provided by LineItem implementations.
 */
class LineItemStore @Inject() (schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  protected def table = schema.lineItems

  // TODO(SER-499): helper queries
  // use CodeType of LineItemType to create LineItem's of the correct type

  def getItemsByCheckoutId(id: Long): Seq[LineItem[_]] = {
    join( schema.lineItems, schema.lineItemTypes ) ( (li, lit) =>
      select(li, lit) on (li._itemTypeId === lit.id and li._checkoutId === id)
    ).toSeq.flatMap { entityTuple =>

      val (itemEntity, itemTypeEntity) = entityTuple

      CodeType(itemTypeEntity._codeType) match {
        case Some(codeType: CodeTypeFactory[LineItemType[_], LineItem[_]]) =>
          println("codeType: " + codeType.name)
          Some(codeType.itemInstance(itemEntity, itemTypeEntity))

        // TODO(SER-499): add logging
        case Some(_) => None
        case None => None
      }
    }
  }

  def findEntityById(id: Long): Option[LineItemEntity] = {
    from(table)( entity =>
      where(entity.id === id) select(entity)
    ).headOption
  }
}




/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

trait HasLineItemEntity extends HasEntity[LineItemEntity] { this: LineItem[_] =>
  def id = _entity.id
}

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

  //
  // Private members
  //
  private def entity = entityLens.asMember(this)

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
    entity.xmap(entity => get(entity))(newProp => set(newProp)).asMember(this)
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
