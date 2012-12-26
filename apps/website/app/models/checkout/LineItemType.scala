package models.checkout

import java.sql.Timestamp
import services.{MemberLens, Time}
import org.squeryl.{Query, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast.LogicalBoolean
import services.db.{KeyedCaseClass, InsertsAndUpdatesAsEntity, HasEntity, Schema}
import scalaz.Lens

import models.enums.{CodeTypeFactory, CodeType, LineItemNature}
import models.{HasCreatedUpdated, SavesCreatedUpdated}
import org.squeryl.annotations.Transient
import com.google.inject.Inject


trait LineItemType[+TransactedT] {
  def id: Long

  // Convenience entity-member accessors
  def description: String
  def nature: LineItemNature
  def codeType: CodeType

  // Serialization
  def toJson: String

  /**
   * Creates one or more [[models.checkout.LineItem]]s from an existing set of resolved
   * ones and a set pending resolution. Both must be provided because to create the correct
   * line items we need to know both what's already there and what's still pending to apply.
   *
   * We need the former because, for example, a 5% discount needs to already know what other
   * items have been purchased in order to know the correct amount.
   *
   * Need the latter because, for example,
   *   * to calculate a 5% discount and 10% discount would theoretically
   *     want to know about each other in order to apply correctly to the right purchase.
   *
   *   * To calculate the Total we would want to know that no further line items are left to calculate.
   *
   *   NOTE: IndexedSeq is an optimization for random access, which isn't really the use case here;
   *         Using a Seq here would allow us to use the list monad instead of option + list
   *
   * @return Seq(new line items) if the line item type was successfully applied.
   *   Otherwise None, to signal that the checkout will try to resolve it again on the next round.
   */
  def lineItems(
    resolvedItems: Seq[LineItem[_]],
    pendingResolution: Seq[LineItemType[_]]
  ): Seq[LineItem[TransactedT]]
}


/**
 * Provide helper queries for getting LineItemType's; persistence is provided by LineItemType implementations.
 * @param schema
 */
class LineItemTypeStore @Inject() (schema: Schema) {
  protected def table = schema.lineItemTypes

  // TODO(SER-499): helper queries
  // use CodeType to create LineItemTypes as correct type
  def getById(id: Long) = {
    table.where(itemType => itemType.id === id).headOption
  }


// TODO(SER-499): remove this when sure it's not being used
//  def entitiesToType[T <: LineItemType[_]](entity: LineItemTypeEntity, itemEntity: LineItemEntity): Option[T] = {
//    CodeType(entity._codeType) match {
//      case Some(codeType: CodeTypeFactory[T, _]) => Some(codeType.typeInstance(entity, itemEntity))
//      case None => None
//    }
//  }
}


/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

trait HasLineItemTypeEntity extends HasEntity[LineItemTypeEntity] { this: LineItemType[_] =>
  def id = _entity.id
}

trait SavesAsLineItemTypeEntity[ModelT <: HasLineItemTypeEntity]
  extends InsertsAndUpdatesAsEntity[ModelT, LineItemTypeEntity]
  with SavesCreatedUpdated[LineItemTypeEntity]
  //extends SavesAsEntity[ModelT, LineItemTypeEntity]
{
  protected def schema: Schema
  override protected val table = schema.lineItemTypes

  override protected def withCreatedUpdated(toUpdate: LineItemTypeEntity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */



/**
 * Gives entity-delegated accessors to LineItemTypes that use LineItemTypeEntity for persistence.
 * Generally you should mix alongside [[models.checkout.LineItemTypeEntityGetters]] and/or
 * [[models.checkout.LineItemTypeEntitySetters]], which automatically generate `description`
 * and `nature` members / mutators for you.
 */
trait LineItemTypeEntityLenses[T <: LineItemType[_]] { this: T with HasLineItemTypeEntity =>
  import MemberLens.Conversions._

  /**
   * Specifies how to get and set the entity into your object. For example:
   *
   * {{{
   *   import scalaz.Lens
   *   case class TaxLineItemType(_entity: EntityLineItemType)
   *     extends LineItemTypeEntityLenses[TaxLineItemType]
   *   {
   *     override protected lazy val entityLens = Lens[TaxLineItemType, LineItemTypeEntity](
   *       get = tax => tax._entity,
   *       set = (tax, newEntity) => tax.copy(_entity=entity)
   *     )
   *   }
   * }}}
   *
   */
  protected def entityLens: Lens[T, LineItemTypeEntity]

  //
  // Private members
  //
  private def entity = entityLens.asMember(this)

  private[checkout] lazy val descField =
    entityField(get = _._desc)(set = desc => entity().copy(_desc=desc))
  private[checkout] lazy val natureField = entityField(
    get = (entity: LineItemTypeEntity) => entity.nature)(
    set = (nature: LineItemNature) => entity().copy(_nature=nature.name))
  private[checkout] lazy val codeTypeField = entityField(
    get = (entity: LineItemTypeEntity) => entity.codeType)(
    set = (codeType: CodeType) => entity().copy(_codeType=codeType.name))


  private def entityField[PropT](get: LineItemTypeEntity => PropT)(set: PropT => LineItemTypeEntity)
  : MemberLens[T, PropT] =
  {
    entity.xmap(entity => get(entity))(newProp => set(newProp)).asMember(this)
  }
}


trait LineItemTypeEntityGetters[T <: LineItemType[_]] { this: T with LineItemTypeEntityLenses[T] =>
  override lazy val description = descField()
  override lazy val nature = natureField()
  override lazy val codeType = codeTypeField()
}


trait LineItemTypeEntitySetters[T <: LineItemType[_]] { this: T with LineItemTypeEntityLenses[T] =>
  lazy val withDescription = descField.set _
  lazy val withNature = natureField.set _
  lazy val withCodeType = codeTypeField.set _
}

trait LineItemTypeEntityGettersAndSetters[T <: LineItemType[_]]
  extends LineItemTypeEntityLenses[T]
  with LineItemTypeEntityGetters[T]
  with LineItemTypeEntitySetters[T] { this: T with HasLineItemTypeEntity => }
