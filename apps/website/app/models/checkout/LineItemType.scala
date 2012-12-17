package models.checkout

import java.sql.Timestamp
import services.{MemberLens, Time}
import org.squeryl.KeyedEntity
import org.squeryl.dsl.ast.LogicalBoolean
import services.db.{SavesAsEntity, Schema}
import scalaz.Lens

import models.enums.{CodeType, LineItemNature}


trait LineItemType[+TransactedT] {
  def id: Long
  def description: String
  def nature: LineItemNature
  def codeType: CodeType
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
 * A row in the LineItemType table
 */
case class LineItemTypeEntity private (
  id: Long,
  _desc: String,
  _nature: String,
  _codeType: String,
  created: Timestamp,
  updated: Timestamp
) extends KeyedEntity[Long] {

  def this(
    desc: String = "",
    nature: LineItemNature,
    codeType: CodeType,
    id: Long = 0,
    created: Timestamp = Time.defaultTimestamp,
    updated: Timestamp = Time.defaultTimestamp
  ) = this(id, desc, nature.name, codeType.name, created, updated)

  def nature = LineItemNature(_nature).get
  def codeType = CodeType(_codeType).get
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