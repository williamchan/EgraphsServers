package models

import javax.persistence.Entity
import play.db.jpa.QueryOn
import org.squeryl.KeyedEntity

/** Vestigial appendage from squeryl testing -- remove this */
case class EgraphsCustomer(
  id: Long = 0,
  credentialId: Long = 0,
  name: Option[String] = Some("")
) extends KeyedEntity[Long]
{
  /*private def credentialQuery: ManyToOne[Credential] = {
    DB.customerToCredential.right(this)
  }*/

  /*def credentials: Credential = {
    DB.customers
    credentialQuery.single
  }*/
}

// Old JPA stuff

/**
 * Persistent entity representing paying purchasers or recipients of the
 * products offered on our service.
 */
@Entity
class Customer extends User

object Customer extends QueryOn[Customer] with UserQueryOn[Customer]
