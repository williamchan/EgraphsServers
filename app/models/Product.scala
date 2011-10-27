package models

import java.util.Date
import play.data.validation.Required
import javax.persistence.{ManyToOne, Entity}
import play.db.jpa.{QueryOn, Model}

@Entity
class Product extends Model with CreatedUpdated {
  @Required
  var name: String = ""

  @Required
  var priceInCents: Int = 0

  @ManyToOne
  @Required
  var seller: Celebrity = null
}

object Product extends QueryOn[Product]
