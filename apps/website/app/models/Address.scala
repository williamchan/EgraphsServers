package models

import com.google.inject.Inject
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{Saves, Schema, KeyedCaseClass}
import org.squeryl.Query

case class AddressServices @Inject()(store: AddressStore,
                                     accountStore: AccountStore)

case class Address(id: Long = 0,
                   accountId: Long = 0,
                   addressLine1: String = "",
                   addressLine2: Option[String] = None,
                   city: String = "",
                   _state: String = "",
                   postalCode: String = "",
                   created: Timestamp = Time.defaultTimestamp,
                   updated: Timestamp = Time.defaultTimestamp,
                   services: AddressServices = AppConfig.instance[AddressServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {
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

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Address.unapply(this)
}

class AddressStore @Inject()(schema: Schema) extends Saves[Address] with SavesCreatedUpdated[Address] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public methods
  //

  def findByAccount(accountId: Long): Query[Address] = {
    from(schema.addresses)((address) => where(address.accountId === accountId) select (address))
  }

  //
  // Saves[Address] methods
  //
  override val table = schema.addresses

  override def defineUpdate(theOld: Address, theNew: Address) = {
    updateIs(
      theOld.accountId := theNew.accountId,
      theOld.addressLine1 := theNew.addressLine1,
      theOld.addressLine2 := theNew.addressLine2,
      theOld.city := theNew.city,
      theOld._state := theNew._state,
      theOld.postalCode := theNew.postalCode,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Address] methods
  //
  override def withCreatedUpdated(toUpdate: Address, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
