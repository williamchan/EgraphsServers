package models.checkout

import services.db.Schema

trait HasEntity[T] { def _entity: T}
trait HasLineItemEntity extends HasEntity[LineItemEntity]
trait HasLineItemTypeEntity extends HasEntity[LineItemTypeEntity]
// Could define more specific traits with entity helper

/**
 * Helper for implementing persistence for new [[models.checkout.LineItemType]]s.
 * Usage:
 * {{{
 *   import models.checkout.LineItemComponent
 *
 *   case class MyLineItemType(_entity: LineItemTypeEntity) extends LineItemType[AnyRef] {
 *     // ...Implementation details...
 *   }
 *
 *   case class MyLineItem(_entity: LineItemEntity) extends LineItem[AnyRef] {
 *     // ...Implementation...
 *   }
 *
 *   object MyLineItemComponent extends LineItemTypeComponent {
 *     override protected val schema: Schema = AppConfig.instance[Schema]
 *
 *     object MyLineItemTypeServices extends SavesAsLineItemTypeEntity[MyLineItemType] {
 *       object Conversions extends EntitySavingConversions
 *
 *       override protected def modelWithNewEntity(model: MyLineItemType, entity: LineItemTypeEntity) = {
 *         MyLineItemType.copy(_entity=entity)
 *       }
 *     }
 *
 *     object MyLineItemServices extends SavesAsLineItemEntity[MyLineItem] {
 *       // ...
 *     }
 *   }
 *
 *   trait MyEndpoint { this: Controller =>
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
trait LineItemComponent {
  protected def schema: Schema

  trait SavesAsLineItemEntity[ModelT <: HasLineItemEntity]
    extends SavesAsEntity[ModelT, LineItemEntity]
  {
    override protected def table = schema.lineItems
  }

  trait SavesAsLineItemTypeEntity[ModelT <: HasLineItemTypeEntity]
    extends SavesAsEntity[ModelT, LineItemTypeEntity]
  {
    override protected def table = schema.lineItemTypes
  }
}