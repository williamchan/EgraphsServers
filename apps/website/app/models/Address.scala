package models

import com.google.inject.Inject
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{HasTransientServices, SavesWithLongKey, Schema, KeyedCaseClass}
import org.squeryl.Query

case class AddressServices @Inject()(store: AddressStore,
                                     accountStore: AccountStore)

case class Address(
  id: Long = 0,
  accountId: Long = 0,
  addressLine1: String = "",
  addressLine2: Option[String] = None,
  city: String = "",
  _state: String = "",
  postalCode: String = "",
  name: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  @transient _services: AddressServices = AppConfig.instance[AddressServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasTransientServices[AddressServices]
{


  //
  // Public methods
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): Address = {
    services.store.save(this)
  }

  def account: Account = {
    services.accountStore.get(accountId)
  }

  def streetAddressString = {
    val address = List(Some(addressLine1), addressLine2).flatten.mkString(" ")
    val stateAndZip = List(_state, postalCode).mkString(" ")
    val streetAddressAsList = name.toList ++ List(address, city, stateAndZip)
    streetAddressAsList.mkString(", ")
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Address.unapply(this)
}

class AddressStore @Inject()(schema: Schema) extends SavesWithLongKey[Address] with SavesCreatedUpdated[Address] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public methods
  //

  def findByAccount(accountId: Long): Query[Address] = {
    from(schema.addresses)((address) => where(address.accountId === accountId) select (address))
  }

  //
  // SavesWithLongKey[Address] methods
  //
  override val table = schema.addresses



  //
  // SavesCreatedUpdated[Address] methods
  //
  override def withCreatedUpdated(toUpdate: Address, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
