package models.checkout

import services.db.{HasEntity, SavesAsEntity, Schema}
import org.joda.money.{CurrencyUnit, Money}
import org.squeryl.KeyedEntity
import services.AppConfig

// TODO(SER-499): compare implementing setters here vs. in lenses
trait HasLineItemEntity extends HasEntity[LineItemEntity] {
  this: LineItem[_] =>
  def amount: Money = Money.of(CurrencyUnit.USD, _entity._amountInCurrency.bigDecimal)
  def id = _entity.id
}

trait HasLineItemTypeEntity extends HasEntity[LineItemTypeEntity] {
  this: LineItemType[_] =>
  def id = _entity.id
}
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
abstract case class Whatever(schema: Schema = AppConfig.instance[Schema])
object LineItemComponent {
  protected val schema: Schema = AppConfig.instance[Schema]

  /**
   * There must be at least two traits -- for LineItem and LineItemType, as far as I can tell at the
   * moment -- because the implicit conversions they each implement need to have different names in
   * order not to shadow each other, which would make it impossible to use both conversions in the
   * same context (leads to annoying work arounds).
   */

  trait SavesAsLineItemEntity[ModelT <: HasLineItemEntity]
    extends SavesAsEntity[ModelT, LineItemEntity]
  {
    override protected val table = schema.lineItems
    val Conversions: LineItemSavingConversions

    trait LineItemSavingConversions extends EntitySavingConversions {
      implicit def itemToSavingDsl(item: ModelT) = new SavingDSL(item)
    }
  }

  trait SavesAsLineItemTypeEntity[ModelT <: HasLineItemTypeEntity]
    extends SavesAsEntity[ModelT, LineItemTypeEntity]
  {
    override protected val table = schema.lineItemTypes
    val Conversions: LineItemTypeSavingConversions

    trait LineItemTypeSavingConversions extends EntitySavingConversions {
      implicit def typeToSavingDsl(itemType: ModelT) = new SavingDSL(itemType)
    }
  }

  trait SavesAsCheckoutEntity extends SavesAsEntity[Checkout, CheckoutEntity] {
    override protected val table = schema.checkouts
    val Conversions: CheckoutSavingConversions

    trait CheckoutSavingConversions extends EntitySavingConversions {
      implicit def checkoutToSavingDsl(checkout: Checkout) = new SavingDSL(checkout)
    }
  }
}