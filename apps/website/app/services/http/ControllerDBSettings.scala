package services.http

import services.db.{TransactionSerializable, TransactionIsolation}

/**
 * Specifies whether a ControllerMethod has a default database transaction. In the case
 * when there is a default database transaction, this also specifies relevant settings.
 */
sealed trait ControllerDBSettings

/**
 * Specifies that a ControllerMethod NOT open and persist a default database connection.
 */
case object WithoutDBConnection extends ControllerDBSettings

/**
 * Specifies that a ControllerMethod open and persist a default database connection.
 *
 * @param dbIsolation the transaction isolation with which to connect to the database
 * @param readOnly true that the database transaction is read-only
 */
case class WithDBConnection(
  dbIsolation: TransactionIsolation = TransactionSerializable,
  readOnly: Boolean = true
) extends ControllerDBSettings
