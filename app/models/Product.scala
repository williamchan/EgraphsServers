package models

import java.util.Date
import play.data.validation.Required
import play.db.jpa.Model
import javax.persistence.{ManyToOne, Entity}

object Product {
  val me = new Product()
}

// TODO(will): Try to make a case class
@Entity
class Product extends Model {

  @Required
  var created: Date = null

  @Required
  var updated: Date = null

  @Required
  var name: String = null

  @Required
  var price: Double = 0.0

  @ManyToOne
  var celebrity: Celebrity = null
}


/* // Anorm
import play.db.anorm._
import play.db.anorm.defaults._

case class Product(productId: Pk[Long] = NotAssigned,
                   created: Date = new Date(),
                   updated: Date = new Date(),
                   celebrityId: Long,
                   name: String
                   /*price: BigDecimal*/) {
}

object Product extends Magic[Product] {

  def apply(celebrity: Celebrity, name: String /*, price: BigDecimal*/): Product = {
    new Product(celebrityId = celebrity.celebrityId.get.get, name = name /*, price = price*/)
  }
}*/
