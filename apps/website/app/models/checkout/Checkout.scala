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
  services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends HasEntity[CheckoutEntity] with KeyedCaseClass[Long]
  with CanInsertAndUpdateAsThroughServices[Checkout, CheckoutEntity]
{

  // uses LineItems and LineItemTypes
  def lineItems: LineItems =  _typesOrItems match {
    case Right(items: LineItems) => items
    case Left(types: LineItemTypes) => resolveTypes(types)
  }

  def lineItemTypes: LineItemTypes = _typesOrItems match {
    case Left(types: LineItemTypes) => types
    case Right(items: LineItems) => items.map(_.itemType)
  }

  def withAdditionalTypes(additionalTypes: LineItemTypes) = {
    this.copy(
      _typesOrItems = Left(additionalTypes ++ lineItemTypes)
    )
  }

  /**
   * Persists this checkout, its types, and its items
   * @return PersistedCheckout of the transacted checkout
   */
  def transact(): Checkout = {
    // TODO(SER-499): require payment/sum of [products, fees, taxes]/total and charges to be zero.
    // TODO(SER-499): make transact take checkout context

    val savedCheckout = services.insert(this)
    val savedItems = savedCheckout.lineItems.map( item => { //item.transact(savedCheckout.id) )
      // DEBUG
      println("before saving, item.checkoutId: " + item.checkoutId)
      val savedItem = item.transact(savedCheckout.id)
      println("after  saving, item.checkoutId: " + savedItem.checkoutId)
      savedItem

    })

    this.copy(savedCheckout._entity, Right(savedItems))
  }


  //
  // KeyedCaseClass members
  //
  override def id = _entity.id
  override def unapplied = ( _entity, lineItemTypes.toSet )


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

  protected def resolveTypes(types: LineItemTypes) = {

    case class ResolutionPass(resolved: LineItems, unresolved: LineItemTypes) {
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

        if (unresolved.isEmpty) this
        else iterate()()
      }
    }

    def resolve(pass: ResolutionPass): LineItems = pass.execute match {
      // TODO(SER-499): add detail to exception
      case ResolutionPass(pass.resolved, _) => throw new Exception("Circular dependency found in LineItemTypes")
      case ResolutionPass(allItems, Nil) => allItems
      case intermediatePass => resolve(intermediatePass)
    }

    val initialPass = ResolutionPass(Seq(), types)
    //resolve(initialPass)

    // DEBUG
    val items = resolve(initialPass)
    println("Checkout.lineItems: " + items.mkString(", "))
    println("Checkout.lineItemTypes: " + lineItemTypes.mkString(", "))
    items
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
    // DEBUG
    println("***Checkout.apply(entity: CheckoutEntity, items: LineItems)***")
    println("entity.id: " + entity.id)
    println("item.checkoutId: " + items.map(_.checkoutId).mkString(" "))


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


