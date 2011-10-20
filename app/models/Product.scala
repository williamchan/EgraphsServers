package models

import java.util.Date
import play.data.validation.Required
import play.db.jpa.Model
import javax.persistence.{ManyToOne, Entity}

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

//object Product {
//  val me = new Product()
//}
