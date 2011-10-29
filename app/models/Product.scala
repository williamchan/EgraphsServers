package models

import java.util.Date
import play.data.validation.Required
import javax.persistence.{ManyToOne, Entity}
import play.db.jpa.{QueryOn, Model}

@Entity
class Product(
  @Required var name: String,
  @Required var priceInCents: Int,
  @Required @ManyToOne var seller: Celebrity
) extends Model with CreatedUpdated

object Product extends QueryOn[Product]
