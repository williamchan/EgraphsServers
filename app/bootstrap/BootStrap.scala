package bootstrap

import play.jobs._
import com.stripe.Stripe
import org.squeryl.{Session, SessionFactory}
import play.Play
import db.DBSession
import libs.Blobs

@OnApplicationStart
class BootStrap extends Job {

  override def doJob() {
    Stripe.apiKey = "pvESi1GjhD9e8RFQQPfeH8mHZ2GIyqQV"

    // Initialize Squeryl persistence
    SessionFactory.concreteFactory =
      Some(() => Session.create(play.db.DB.getConnection, db.Adapter.current))

    // Initialize S3 or fs-based blobstore
    Blobs.init()

    if (Play.id == "test") {
      TestModeBootstrap.run()
    }
  }

}

private object TestModeBootstrap {
  def run() {
    createNewSchema()
    Blobs.scrub()
  }

  def createNewSchema() {
    // drop and re-create the database definition, logging the
    // creation SQL.
    import org.squeryl.PrimitiveTypeMode._
    DBSession.init()
    inTransaction {
      // db.Schema.printDdl((ddl) => println(ddl))
      db.Schema.scrub()
    }
  }
}