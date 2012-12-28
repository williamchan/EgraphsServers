package models.checkout

import models.CashTransaction
import models.enums.{CodeType, LineItemNature}
import org.joda.money.Money
import scalaz.Lens
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import com.google.inject.Inject
import play.api.libs.json.Json


case class CashTransactionLineItemType protected (
  _entity: LineItemTypeEntity,
  stripeToken: String,
  services: CashTransactionLineItemTypeServices = AppConfig.instance[CashTransactionLineItemTypeServices]
) extends LineItemType[CashTransaction] with HasLineItemTypeEntity
  with LineItemTypeEntityGettersAndSetters[CashTransactionLineItemType]
  with CanInsertAndUpdateAsThroughServices[CashTransactionLineItemType, LineItemTypeEntity]
{
  override def toJson: String = {
    // TODO(SER-499): more json
    ""
  }

  def lineItems(resolvedItems: Seq[LineItem[_]], pendingResolution: Seq[LineItemType[_]]) = {
    resolvedItems.find( item => item.itemType.codeType == CodeType.Total) match {
      case Some(total: TotalLineItem) => Seq( CashTransactionLineItem(this, total.amount.negated) )
      case _ => Nil
    }
  }

  override protected lazy val entityLens = Lens[CashTransactionLineItemType, LineItemTypeEntity] (
    get =  charge => charge._entity,
    set = (charge, entity) => charge.copy(entity)
  )
}

object CashTransactionLineItemType {
  val nature = LineItemNature.Charge
  val codeType = CodeType.CashTransaction

  def apply(stripeToken: String): CashTransactionLineItemType = {
    CashTransactionLineItemType(
      LineItemTypeEntity(entityDescriptionAsJsonString(stripeToken), nature, codeType),
      stripeToken
    )
  }

  def apply(entity: LineItemTypeEntity, itemEntity: LineItemEntity): CashTransactionLineItemType = {
    val stripeToken = stripeTokenOptionFromEntity(entity)
      .getOrElse(throw new IllegalArgumentException("Stripe token could not be parsed from entity."))
    CashTransactionLineItemType(entity, stripeToken)
  }

  def stripeTokenOptionFromEntity(entity: LineItemTypeEntity) = {
    (Json.parse(entity._desc) \ jsonStripeTokenKey).asOpt[String]
  }

  protected val jsonStripeTokenKey = "StripeToken"
  protected def entityDescriptionAsJsonString(token: String) = {
    Json.stringify(
      Json.toJson(
        Map(jsonStripeTokenKey -> Json.toJson(token))
      )
    )
  }

}





case class CashTransactionLineItemTypeServices @Inject() (
  schema: Schema
) extends SavesAsLineItemTypeEntity[CashTransactionLineItemType] {

  // TODO(SER-499): determine what additional services are needed

  override protected def modelWithNewEntity(charge: CashTransactionLineItemType, entity: LineItemTypeEntity) = {
    charge.copy(_entity = entity)
  }
}