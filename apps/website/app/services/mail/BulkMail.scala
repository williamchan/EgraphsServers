package services.mail

import org.scalacheck.Properties
import com.google.inject.{Inject, Provider}
import services.Utils
import services.http.PlayConfig

trait BulkMail {
  def subscribe(email: String)
}

class BulkMailProvider @Inject()(@PlayConfig playConfig: Properties, utils: Utils) extends Provider[BulkMail]
{
  def get() : BulkMail = {
    //Inspect properties and return the proper BulkMail
    new MockBulkMail
  }
}

private[mail] class MockBulkMail extends BulkMail {
  override def subscribe(email: String) {
    play.Logger.info("Subscribed " + email + "\n")
  }
}