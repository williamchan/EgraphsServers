package models

import com.google.inject.{Provider, Inject}
import services.db.{SavesWithStringKey, SavesWithLongKey, Schema}
import java.sql.Timestamp

class UsernameHistoryStore @Inject() (
  schema: Schema
) extends SavesWithStringKey[Username] with SavesCreatedUpdated[ Username]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public members
  //

  /**
   * Gets the customer's current username.
   * @param customer
   * @return
   */
  def findCurrentByCustomer(customer: Customer): Option[Username] = {
    findCurrentByCustomerId(customer.id)
  }

  /**
   * Gets the customer's current username.
   * @param customerId
   * @return
   */
  def findCurrentByCustomerId(customerId: Long): Option[Username] = {
    val allUsernames = findAllByCustomerId(customerId)

    def permanentUsername = {username: Username => username.isPermanent}
    def anyValidUsername = {username: Username => !username.isRemoved}

    if (allUsernames.exists(permanentUsername)) {
      allUsernames.find(permanentUsername)
    } else if (allUsernames.exists(anyValidUsername)) {
      allUsernames.find(anyValidUsername)
    } else {
      None
    }
  }

  /**
   * Gets all the usernames that are or were once associated with the customer.
   * @param customer
   * @return
   */
  def findAllByCustomer(customer: Customer): Seq[Username] = {
    from(schema.usernameHistories)((history) => where(history.customerId === customer.id) select (history)).toSeq
  }

  /**
   * Gets all the usernames that are or were once associated with the customer.
   * @param customerId
   * @return
   */
  def findAllByCustomerId(customerId: Long): Seq[Username] = {
    from(schema.usernameHistories)((history) => where(history.customerId === customerId) select (history)).toSeq
  }

  //
  // SavesWithLongKey[UsernameHistory] methods
  //
  override val table = schema.usernameHistories



  //
  // SavesCreatedUpdated[UsernameHistory] methods
  //
  override def withCreatedUpdated(toUpdate: Username, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}