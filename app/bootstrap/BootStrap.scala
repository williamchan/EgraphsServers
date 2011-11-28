package bootstrap

import play.jobs._
import com.stripe.Stripe
import org.squeryl.{Session, SessionFactory}
import play.Play
import db.DBSession
import libs.{TempFile, Blobs}
import io.Source
import java.io.{File, FileOutputStream, PrintWriter}

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

/**
 * Bootstrap code for when we're running in test mode
 */
private object TestModeBootstrap {
  import org.squeryl.PrimitiveTypeMode._

  def run() {
    bootstrapDatabase()
  }

  private def bootstrapDatabase() {
    DBSession.init()
    inTransaction {
      if (schemaHasChanged) {
        play.Logger.info(
          "Detected change in database schema; scrubbing DB and blobstore. (You can view the current schema at " +
           schemaFile.getAbsolutePath + ")"
        )
        createNewSchema()
        Blobs.scrub()
      }
    }
  }

  /**
   * Drops and re-creates the database definition, logging the creation SQL
   * to schemaFile.
   */
  private def createNewSchema() {
    // drop and re-create the database definition, logging the
    // creation SQL in a temporary file.    
    printDdlToFile(schemaFile)

    db.Schema.scrub()
  }

  /** Prints the database schema to the provided file. */
  private def printDdlToFile(file: File) {
    file.delete()
    val ddlStream = new FileOutputStream(file)
    val ddlWriter = new PrintWriter(ddlStream)

    db.Schema.printDdl(ddlWriter)
    ddlWriter.close()
    ddlStream.close()
  }

  /**
   * Returns true that the schema has changed since the last time
   * the server was run.
   */
  private def schemaHasChanged: Boolean = {
    if (schemaFile.exists()) {
      val prevDdl = Source.fromFile(schemaFile).mkString

      prevDdl != db.Schema.ddl 
    }
    else {
      // No schema file existed, so the schema is different whatever it is.
      true
    }
  }

  /** Location for log of the database schema SQL */
  def schemaFile = TempFile.named("schema.sql")
}