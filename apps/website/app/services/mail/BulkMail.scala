package services.mail

import com.google.inject.{Inject, Provider}
import services.Utils


import services.http.PlayConfig
import java.util.Properties


trait BulkMail {
  def subscribeNew(listId: String, email: String)
}

class BulkMailProvider @Inject()(@PlayConfig playConfig: Properties, utils: Utils) extends Provider[BulkMail]
{
  def get() : BulkMail = {
    //Inspect properties and return the proper BulkMail
    new MockBulkMail(utils)
  }
}

private[mail] case class MockBulkMail (utils: Utils) extends BulkMail
{
  override def subscribeNew(listId: String, email: String) = {
    play.Logger.info("Subscribed " + email + " to email list: " + listId + "\n")
  }
}

