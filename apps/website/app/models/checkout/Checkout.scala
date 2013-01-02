package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums._
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db._
import models.{HasCreatedUpdated, SavesCreatedUpdated}
import com.google.inject.Inject

package object checkout {
  /** For _domainEntityId if domain object doesn't get persisted; ex: Tax */
  val UnusedDomainEntity: Long = -1

  /** For id's of line items and line item types that don't get persisted; ex: Subtotal */
  val Unpersisted: Long = -2

  val UnsavedEntity = 0

  type LineItems = Seq[LineItem[_]]
  type LineItemTypes = Seq[LineItemType[_]]
}

import checkout._




case class CheckoutEntity(
  id: Long = 0,
  customerId: Long = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = CheckoutEntity.unapply(this)
}


case class Checkout (
  _entity: CheckoutEntity,
  _typesOrItems: Either[LineItemTypes, LineItems],
  _additionalTypes: LineItemTypes = Nil,
  services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends HasEntity[CheckoutEntity, Long] with KeyedCaseClass[Long]
  with CanInsertAndUpdateAsThroughServices[Checkout, CheckoutEntity]
{

  // uses LineItems and LineItemTypes
  lazy val lineItems: LineItems =  _typesOrItems match {
    case Right(items: LineItems) => resolveTypes(_additionalTypes, items)
    case Left(types: LineItemTypes) => resolveTypes(types)
  }

  lazy val lineItemTypes: LineItemTypes = _typesOrItems match {
    case Left(types: LineItemTypes) => types
    case Right(items: LineItems) => _additionalTypes ++ items.map(_.itemType)
  }


  /**
   * To be used for adding any line item types (products, discounts, refunds, etc)
   * @param additionalTypes -- to be added to this checkout
   * @return If transacted, checkout with additionalTypes in _additionalTypes; otherwise, checkout
   *         with _additionalTypes added to lineItemTypes.
   */
  def withAdditionalTypes(additionalTypes: LineItemTypes) = {
    if (id > 0) {
      this.copy(_additionalTypes = additionalTypes ++ _additionalTypes)
    } else {
      this.copy(_typesOrItems = Left(additionalTypes ++ lineItemTypes))
    }
  }

  /**
   * Persists this checkout, its types, and its items
   * @return PersistedCheckout of the transacted checkout
   */
  def transact(): Checkout = {
    // TODO(SER-499): require payment/sum of [products, fees, taxes]/total and charges to be zero.
    // TODO(SER-499): make transact take checkout context
    if (id > 0) {

      // calculate new total
      // require charge of difference between previous total and current total
      // Has all tax, fee, & summary types, will apply them to entire checkout
      //   -should they be applied only to additional types somehow? conflicts with refunds
      //   -new taxes/fees otherwise need to be matched to old ones to calculate differences
      val tempCheckout = new Checkout(CheckoutEntity(), Left(lineItemTypes))
      val newTotal = tempCheckout.total

      // transact new types' items
      //   -filter items that aren't transacted
      // transact new fees, taxes, and charge
      // TODO(SER-499): make sure summaries don't assume there's only one tax items


      this

    } else {
      // Completely untransacted
      val savedCheckout = services.insert(this)
      val savedItems = savedCheckout.lineItems.map( item => item.transact(savedCheckout.id) )

      this.copy(savedCheckout._entity, Right(savedItems))
    }
  }


  //
  // KeyedCaseClass members
  //
  override def id = _entity.id
  override def unapplied = ( _entity, lineItems.toSet )


  //
  // utility members
  //
  def subtotal: SubtotalLineItem = lineItemsOfCodeType(CodeType.Subtotal).head
  def total: TotalLineItem = lineItemsOfCodeType(CodeType.Total).head

  def flattenLineItems: LineItems = {
    for (lineItem <- lineItems; flattened <- lineItem.flatten) yield flattened
  }

  def lineItemsOfCodeType[ LIT <: LineItemType[_], LI  <: LineItem[_] ] (
    codeType: CodeTypeFactory[LIT, LI]
  ): Seq[LI] = {
    for (item <- lineItems if item.codeType == codeType) yield item.asInstanceOf[LI]
  }

  protected def resolveTypes(types: LineItemTypes, preexistingItems: LineItems = Seq()) = {

    case class ResolutionPass(resolved: LineItems = preexistingItems, unresolved: LineItemTypes) {
      def execute: ResolutionPass = {
        /**
         * Iterates over unresolved in linear time on average (only counting iteration time)
         *
         * @param acc - accumulator of line items, should include previously resolved
         * @param before - item types preceding current type in iteration from list of all types
         * @param curr - current line item type being iterated over
         * @param after - remaining line item types to be iterated over
         * @return - ResolutionPass of resulting resolved items and unresolved types
         */
        def iterate(acc: LineItems = resolved)(
          before: LineItemTypes = Nil,
          curr: LineItemType[_] = unresolved.head,
          after: LineItemTypes = unresolved.tail
        ): ResolutionPass = (after, curr.lineItems(acc, before ++ after)) match {
          case (Nil, Nil) => ResolutionPass(acc, curr +: before)
          case (Nil, res) => ResolutionPass(res ++ acc,  before)
          case (next :: rest, Nil) => iterate(acc)(curr +: before, next, rest)
          case (next :: rest, res) => iterate(res ++ acc)( before, next, rest)
        }

        if (unresolved.isEmpty) this  // no additional to resolve
        else iterate()()              // iterate over unresolved
      }

      override def equals(that: Any) = that match {
        case ResolutionPass(thoseResolved, thoseUnresolved) =>
          (resolved.length, unresolved.length) == (thoseResolved.length, thoseUnresolved.length)
        case _ => false
      }
    }

    def resolve(pass: ResolutionPass): LineItems = {
      pass.execute match {
        case ResolutionPass(allItems, Nil) => allItems
        case unchangedPass if (unchangedPass == pass) =>
          throw new Exception("Failed to resolve line items, circular dependency detected.")
        case intermediatePass => resolve(intermediatePass)
      }
    }

    resolve(ResolutionPass(unresolved = types))
  }
}

object Checkout {
  import checkout._

  // Create
  def apply(types: LineItemTypes, zipcode: String) = {
    val maybeZipcode = if (zipcode.isEmpty) None else Some(zipcode)
    val typesWithTaxesAndSummaries = types ++ taxesAndSummariesByZip(maybeZipcode)

    new Checkout(CheckoutEntity(), Left(typesWithTaxesAndSummaries))
  }

  // Restore
  def apply(entity: CheckoutEntity, items: LineItems) = {
    assert(!items.exists(item => item.checkoutId != entity.id))
    new Checkout(entity, Right(itemsWithSummaries(items)))
  }


  /**
   * Helper for generating taxes and summaries.
   * TODO: add fees, such as shipping, later.
   *
   * @param maybeZipcode - None defaults to no taxes
   * @return Sequence of subtotal, taxes, and total (in that order)
   */
  protected def taxesAndSummariesByZip(maybeZipcode: Option[String]): LineItemTypes = {
    Seq(SubtotalLineItemType) ++
      TaxLineItemType.getTaxesByZip(maybeZipcode.getOrElse(TaxLineItemType.noZipcode)) ++
      Seq(TotalLineItemType)
  }

  protected def itemsWithSummaries(items: LineItems): LineItems = {
    val nonSummaries = items.filter (item => item.nature != LineItemNature.Summary )

    val subtotal = items.find( item => item.codeType == CodeType.Subtotal )
      .getOrElse(SubtotalLineItemType.lineItems(nonSummaries, Seq()).head)

    val total = items.find( item => item.codeType == CodeType.Total )
      .getOrElse(TotalLineItemType.lineItems(nonSummaries, Seq()).head)

    subtotal +: (total +: nonSummaries)
  }
}



//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\


class CheckoutServices @Inject() (
  schema: Schema,
  lineItemStore: LineItemStore
) extends InsertsAndUpdatesAsEntity[Checkout, CheckoutEntity]
  with SavesCreatedUpdated[CheckoutEntity]
{
  override protected def table = schema.checkouts

  override def modelWithNewEntity(checkout: Checkout, entity: CheckoutEntity) = checkout.copy(entity)

  override def withCreatedUpdated(toUpdate: CheckoutEntity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }

  def findById(id: Long): Option[Checkout] = table.lookup(id).map { entity =>
    Checkout(entity, lineItemStore.getItemsByCheckoutId(entity.id))
  }

  def getCheckoutsByCustomerId(id: Long): Seq[Checkout] = {
    Nil
  }
}

// TODO(SER-499): fill in the gaps
//class CheckoutStore @Inject() (schema: Schema, lineItemStore: LineItemStore) {
//
//  def findById(id: Long): Option[Checkout] = {
//    // val entity = schema.checkouts.where(...)
//    // val lineItems = lineItemStore.getItemsByCheckoutId(id)
//    None
//  }
//
//  def getCheckoutsByCustomerId(id: Long): Seq[Checkout] = {
//    Nil
//  }
//}

//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\


