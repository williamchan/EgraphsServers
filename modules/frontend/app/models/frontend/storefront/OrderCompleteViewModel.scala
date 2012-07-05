package models.frontend.storefront

import java.util.Date
import org.joda.money.Money

case class OrderCompleteViewModel (
  orderDate: Date,
  orderNumber: Long,
  buyerName: String,
  buyerEmail: String,
  ownerName: String,
  ownerEmail: String,
  celebName: String,
  productName: String,
  totalPrice: Money,
  guaranteedDeliveryDate: Date,
  cancelOrderUrl: String
)
