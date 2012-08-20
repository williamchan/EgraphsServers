package models

import com.google.inject.{Provider, Inject}
import services.db.{SavesStringKey, SavesAll, Saves, Schema}
import java.sql.Timestamp

/**
 * Created with IntelliJ IDEA.
 * User: myyk
 * Date: 8/15/12
 * Time: 7:06 PM
 * To change this template use File | Settings | File Templates.
 */

class UsernameHistoryStore @Inject() (
  schema: Schema
) extends SavesStringKey[UsernameHistory] with SavesCreatedUpdatedAll[String, UsernameHistory]
{
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public members
  //

  def findByUsername(username: String): Seq[UsernameHistory] = {
    from(schema.usernameHistories)((history) => where(lower(history.username) === username.toLowerCase) select (history)).toSeq
  }

  //
  // Saves[UsernameHistory] methods
  //
  override val table = schema.usernameHistories

  override def defineUpdate(theOld: UsernameHistory, theNew: UsernameHistory) = {
    updateIs(
      theOld.username  := theNew.username,
      theOld.customerId := theNew.customerId,
      theOld.isPermanent  := theNew.isPermanent,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[UsernameHistory] methods
  //
  override def withCreatedUpdated(toUpdate: UsernameHistory, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}