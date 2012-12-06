package models.checkout

import java.sql.Timestamp
import services.{MemberLens, Time}
import org.squeryl.KeyedEntity
import services.db.{SavesAsEntity, Schema}
import scalaz.Lens

trait LineItemType[+TransactedT] {
   def description: String

   def nature: LineItemNature

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
    * @return Some(new sequence of line items) if the line item type was successfully applied.
    *   Otherwise None, to signal that the checkout will try to resolve it again on the next round.
    */
   def lineItems(
     resolvedItems: IndexedSeq[LineItem[_]] = IndexedSeq(),
     pendingResolution: IndexedSeq[LineItemType[_]]
   ): Option[IndexedSeq[LineItem[TransactedT]]]
 }



/**
 * A row in the LineItemType table
 */
case class LineItemTypeEntity (
  id: Long = 0L,
  _desc: String = "",
  _nature: String = LineItemNature.Product.name,
  codeType: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedEntity[Long]
{
  def nature = LineItemNature(_nature).get
  def withNature(newNature: LineItemNature) = this.copy(_nature=newNature.name)
}


//
// Persistence
//

/**
 * Helper for implementing persistence for new [[models.checkout.LineItemType]]s.
 * Usage:
 * {{{
 *   import models.checkout.LineItemTypeComponent
 *
 *   case class MyLineItemType(_entity: LineItemTypeEntity) extends LineItemType[AnyRef] {
 *     // ...Implementation details...
 *   }
 *
 *   trait MyLineItemTypeComponent { this: LineItemTypeEntityComponent =>
 *     object MyLineItemTypeServices extends SavesAsLineItemTypeEntity[MyLineItemTypeServices] {
 *       object Conversions extends EntitySavingConversions
 *
 *       override protected def modelWithNewEntity(model: MyLineItemType, entity: LineItemTypeEntity) = {
 *         MyLineItemType.copy(_entity=entity)
 *       }
 *     }
 *   }
 *
 *   trait MyEndpoint { this: Controller with MyLineItemTypeComponent =>
 *     import MyLineItemTypeServices.Conversions._
 *
 *     def serveSomeRequest = Action {
 *       // Save a new, blank instance
 *       MyLineItemType(LineItemTypeEntity()).create()
 *
 *       Ok
 *     }
 *   }
 * }}}
 */
trait LineItemTypeEntityComponent {
  protected def schema: Schema

  trait SavesAsLineItemTypeEntity[ModelT <: {def _entity: LineItemTypeEntity}]
    extends SavesAsEntity[ModelT, LineItemTypeEntity]
  {
    override protected def table = schema.lineItemTypes
    override protected def modelToEntity(model: ModelT) = model._entity
  }
}


/**
 * Gives entity-delegated accessors to LineItemTypes that use LineItemTypeEntity for persistence.
 * Generally you should mix alongside [[models.checkout.LineItemTypeEntityGetters]] and/or
 * [[models.checkout.LineItemTypeEntitySetters]], which automatically generate `description`
 * and `nature` members / mutators for you.
 */
trait LineItemTypeEntityLenses[T <: LineItemType[_]] { this: T =>
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

  private[checkout] lazy val descField = entityField(get = _._desc)(set = desc => entity().copy(_desc=desc))
  private[checkout] lazy val natureField = entityField(get = _.nature)(set = nature => entity().withNature(nature))


  private def entityField[PropT](get: LineItemTypeEntity => PropT)(set: PropT => LineItemTypeEntity)
  : MemberLens[T, PropT] =
  {
    entity.xmap(entity => get(entity))(newProp => set(newProp)).asMember(this)
  }
}


trait LineItemTypeEntityGetters[T <: LineItemType[_]] { this: T with LineItemTypeEntityLenses[T] =>
  override lazy val description = descField()
  override lazy val nature = natureField()
}


trait LineItemTypeEntitySetters[T <: LineItemType[_]] { this: T with LineItemTypeEntityLenses[T] =>
  lazy val withDescription = descField.set _
  lazy val withNature = natureField.set _
}