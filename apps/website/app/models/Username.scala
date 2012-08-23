package models

import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.KeyedCaseClass
import org.squeryl.annotations.Column

/**
 * Persistent entity representing a customer's username.  This may not be their current user name, but one that they
 * once had at the very least.  There should be at most 1 Username per customerId that isPermenant.
 */
case class Username(
  @Column("username")
  id: String = "",
  customerId: Long = 0L,
  isPermanent: Boolean = false,
  isRemoved: Boolean = false, // a username may be removed by admins if the name is too offensive for example
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[String] with HasCreatedUpdated {

  def username = id

  // Public methods
  //
  def save(): Username = {
    RichUsername(this).save()
  }

  //
  // KeyedCaseClass[String] methods
  //
  override def unapplied = {
    Username.unapply(this)
  }
}

case class RichUsername(username: Username) {
  implicit def username2RichUsername(username: Username): RichUsername = {
    RichUsername(username)
  }

  val services = AppConfig.instance[CustomerServices]

  /** Retrieves the UsernameHistory's Customer from the database */
  def customer: Customer = {
    services.customerStore.findById(username.customerId).get
  }

  /**
   * True if the underlying Username can be permanent without breaking the data model's constraints, false otherwise.
   */
  lazy val canBePermanent: Boolean = {
    val allUsernames = services.usernameHistoryStore.findAllByCustomerId(username.customerId)
    val allOtherUsernames = allUsernames.filterNot(history => username.username == history.username)
    val hasPermanent = allOtherUsernames.exists(username => username.isPermanent)
    !hasPermanent
  }

  def save(): Username = {
    if(username.isPermanent && !canBePermanent) {
      throw new Exception("There can only be one permenant username for each customerId.  Cannot make this username permenant.")
    }

    services.usernameHistoryStore.save(username)
  }
}