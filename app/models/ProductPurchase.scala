package models

case class ProductPurchase (order: Order, transaction: CashTransaction) {
  def save(): ProductPurchase = {
    val savedTransaction = transaction.save()
    val savedOrder = order.save()
    this.copy(savedOrder, savedTransaction)
  }
}