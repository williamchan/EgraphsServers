package models.checkout

import Conversions._
import java.sql.Timestamp
import models.enums._
import models.SavesCreatedUpdated
import scalaz.Lens
import services.{AppConfig, MemberLens}
import services.db._
import play.api.libs.json.{Json, JsValue}


/**
 * LineItemTypes capture domain data needed by the corresponding line item and domain object to be
 * built. This will generally be form data.
 *
 * Next, the LineItemType represent the _intent_ to include an item in a checkout -- it is generally
 * not barred from being added to a checkout, but does not guarantee it will generate anything for
 * the checkout it belongs to (the exceptions at the moment are summaries types, which are managed
 * within the checkout so they don't need to, and should not, be added to a checkout manually).
 *
 * @tparam T type of domain object represented by this LineItemType
 */
trait LineItemType[+T] extends HasLineItemNature with HasCodeType {

  def id: Long
  def description: String
  def toJson: JsValue = Json.toJson {
    Map(
      "id" -> Json.toJson(id),
      "codeType" -> Json.toJson(codeType.name)
    )
  }

  /**
   * Creates one or more [[models.checkout.LineItem]]s from an existing set of resolved
   * ones and a set pending resolution. Both must be provided because to create the correct
   * line items we need to know both what's already there and what's still pending to apply.
   *
   * We need the former because, for example, a 5% discount needs to already know what other
   * items have been purchased in order to know the correct amount.
   *
   * Need the latter because, for example,
   *   * to calculate a 5% discount and 10% discount would theoretically want to know about each
   *     other in order to apply correctly to the right purchase (e.g. should they stack or apply
   *     in sequence, and if so what order? Or which should apply if they cannot be combined?).
   *
   *   * To calculate the Total we would want to know that no needed line items are left to calculate.
   *
   * @return Some(Seq(...)) if the line item type was successfully applied. Otherwise None.
   *         Note that a type may determine that it should not resolve any items, as opposed to not
   *         being ready to resolve, in which case it should return Some(Nil).
   */
  def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes)
  : Option[LineItems]
}


/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


trait HasLineItemTypeEntity[T <: LineItemType[_]] extends HasEntity[LineItemTypeEntity, Long] { this: T => 
  def withEntity(entity: LineItemTypeEntity): T
}

trait SavesAsLineItemTypeEntityThroughServices[
  T <: LineItemType[_] with HasLineItemTypeEntity[T],
  ServicesT <: SavesAsLineItemTypeEntity[T]
] extends CanInsertAndUpdateEntityThroughTransientServices[T, LineItemTypeEntity, ServicesT] { this: T with Serializable => }



/** Service trait, enables saving of LineItemTypeEntities through their enclosing LineItemType */
trait SavesAsLineItemTypeEntity[T <: LineItemType[_] with HasLineItemTypeEntity[T]]
  extends InsertsAndUpdatesAsEntity[T, LineItemTypeEntity]
  with SavesCreatedUpdated[LineItemTypeEntity]
{
  protected def schema: Schema
  override protected def table = schema.lineItemTypes

  override protected def modelWithNewEntity(model: T, entity: LineItemTypeEntity) = model.withEntity(entity)
  override protected def withCreatedUpdated(toUpdate: LineItemTypeEntity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}

/** For adding queries for LineItemTypes by id */
trait QueriesAsLineItemTypeEntity[T <: LineItemType[_] with HasLineItemTypeEntity[T]]
  extends QueriesAsEntity[T, LineItemTypeEntity, Long]
{
  protected def schema: Schema
}


/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */



/**
 * Gives entity-delegated accessors to LineItemTypes that use LineItemTypeEntity for persistence.
 * Generally you should mix alongside [[models.checkout.LineItemTypeEntityGetters]] and/or
 * [[models.checkout.LineItemTypeEntitySetters]], which automatically generate members / mutators
 * for the fields below.
 */
trait LineItemTypeEntityLenses[T <: LineItemType[_]] { this: T with HasLineItemTypeEntity[T] =>
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

  def entity = entityLens.asMemberOf(this)

  //
  // Private members
  //
  private[checkout] lazy val descField = entityField(
    getter = entity._desc,
    setter = (desc: String) => entity().copy(_desc = desc))

  private[checkout] lazy val natureField = entityField[LineItemNature](
    getter = entity.nature,
    setter = entity.withNature(_))

  private[checkout] lazy val codeTypeField = entityField[CheckoutCodeType](
    getter = entity.codeType,
    setter = entity.withCodeType(_))

  /** helper for making member lenses */
  private def entityField[PropT](getter: => PropT, setter: PropT => LineItemTypeEntity) = MemberLens[T, PropT](this)(
    getter = getter,
    setter = (prop: PropT) => entity.set( setter(prop) )
  )
}


trait LineItemTypeEntityGetters[T <: LineItemType[_]] { this: T =>

  def _entity: LineItemTypeEntity
  override def id = _entity.id
  override lazy val description = _entity._desc
  override lazy val nature = _entity.nature
  override lazy val codeType = _entity.codeType
}


trait LineItemTypeEntitySetters[T <: LineItemType[_]] extends LineItemTypeEntityLenses[T] {
  this: T with HasLineItemTypeEntity[T] =>

  override def withEntity(newEntity: LineItemTypeEntity) = entity.set(newEntity)
  lazy val withDescription = descField.set _
  lazy val withNature = natureField.set _
  lazy val withCodeType = codeTypeField.set _
}

trait LineItemTypeEntityGettersAndSetters[T <: LineItemType[_]]
  extends LineItemTypeEntityLenses[T]
  with LineItemTypeEntityGetters[T]
  with LineItemTypeEntitySetters[T] { this: T with HasLineItemTypeEntity[T] => }
