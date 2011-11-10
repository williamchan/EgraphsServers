package bootstrap

import play.jobs._
import com.stripe.Stripe
import org.squeryl.{Session, SessionFactory}
import play.Play

@OnApplicationStart
class BootStrap extends Job {

  override def doJob() {
    Stripe.apiKey = "pvESi1GjhD9e8RFQQPfeH8mHZ2GIyqQV"
    preparePersistence()
  }

  /** Bootstraps project persistence using Squeryl. */
  private def preparePersistence() {
    SessionFactory.concreteFactory =
      Some(() => Session.create(play.db.DB.getConnection, db.Adapter.current))

    // In test mode drop and re-create the database definition, logging the
    // creation SQL.
    if (Play.id == "test") {
      import org.squeryl.PrimitiveTypeMode._
      inTransaction {
        // db.Schema.printDdl((ddl) => println(ddl))
        db.Schema.scrub()
      }
    }
  }
}