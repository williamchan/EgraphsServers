package models

import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.KeyedCaseClass
import org.squeryl.annotations.Column

/**
 * Created with IntelliJ IDEA.
 * User: myyk
 * Date: 8/15/12
 * Time: 6:49 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Persistent entity representing history of a customer's username.
 */
case class UsernameHistory(
  @Column("username")
  id: String = "",
  customerId: Long = 0L,
  isPermanent: Boolean = false,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CustomerServices = AppConfig.instance[CustomerServices]
) extends KeyedCaseClass[String] with HasCreatedUpdated {

  def username = id

  // Public methods
  //
  def save(): UsernameHistory = {
    require(!username.isEmpty, "UsernameHistory: username must be specified")
    require(customerId != 0L, "UsernameHistory: customerId must be specified")
    services.usernameHistoryStore.save(this)
  }

  /** Retrieves the UsernameHistory's Customer from the database */
  def customer: Customer = {
    services.customerStore.findById(customerId).get
  }

  //
  // KeyedCaseClass[String] methods
  //
  override def unapplied = {
    UsernameHistory.unapply(this)
  }
}
