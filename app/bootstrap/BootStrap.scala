package bootstrap

import play.jobs._
import com.stripe.Stripe
import org.squeryl.{Session, SessionFactory}
import db.DBSession
import io.Source
import java.io.{File, PrintWriter}
import libs.{Payment, Utils, TempFile, Blobs}
import libs.Blobs.Conversions._
import models.{Account, Celebrity}
import math.BigDecimal._

@OnApplicationStart
class BootStrap extends Job {

  override def doJob() {
    // Initialize payment system
    Stripe.apiKey = Payment.StripeKey.secret

    // Initialize Squeryl persistence
    SessionFactory.concreteFactory =
      Some(() => Session.create(play.db.DB.getConnection, db.DBAdapter.current))

    // Initialize S3 or fs-based blobstore
    Blobs.init()

    // Some additional test-mode setup
    // TODO: make this only happen in test mode once the alpha is over
    // if (Play.id == "test") {
    TestModeBootstrap.run()
    // }

    createAlphaTesters()
  }

  private def createAlphaTesters() {
    createCelebrity("Erem", "Boto", "erem@egraphs.com")
    createCelebrity("Andrew", "Smith", "andrew@egraphs.com")
    createCelebrity("David", "Auld", "david@egraphs.com")
    createCelebrity("Eric", "Feeny", "eric@egraphs.com")
    createCelebrity("Will", "Chan", "will@egraphs.com")
    createCelebrity("Zach Apter", "", "zachapter@gmail.com")
    createCelebrity("Brian", "Auld", "bauld@raysbaseball.com")
    createCelebrity("Michael", "Kalt", "mkalt@raysbaseball.com")
    createCelebrity("Matt", "Silverman", "msilverman@raysbaseball.com")
    createCelebrity("Gabe", "Kapler", "gabe@egraphs.com")
  }

  private def createCelebrity(firstName: String, lastName: String, email: String) {
    println("Creating Celebrity " + email + " ...")
    
    val celebrity = Celebrity(
      firstName = Some(firstName),
      lastName = Some(lastName),
      publicName = Some("Alpha " + firstName),
      description = Some("Tyson 15")
    ).save()

    Account(email = email,
      celebrityId = Some(celebrity.id)
    ).withPassword("herp").right.get.save()
    celebrity.saveWithProfilePhoto(new File("./test/files/E.jpg"))

    celebrity.newProduct.copy(
      priceInCurrency = 50,
      name = firstName + "'s Alpha Product A",
      description = "Today's Sriracha is tomorrow's salsa"
    ).save()

    celebrity.newProduct.copy(
      priceInCurrency = 100,
      name = firstName + "'s Alpha Product B",
      description = "Help me... help YOU..."
    ).save()

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
      if ((!db.Schema.isInPlace) || schemaHasChanged) {
        play.Logger.info(
          """Detected either lack of database schema or change thereof. Scrubbing DB and blobstore.
          (You can view the current schema at """ + schemaFile.getAbsolutePath + ")"
        )
        createNewSchema()
        Blobs.scrub()
      }
    }
    DBSession.commit()
  }

  /**
   * Drops and re-creates the database definition, logging the creation SQL
   * to schemaFile.
   */
  private def createNewSchema() {
    // drop and re-create the database definition, logging the
    // creation SQL to a temporary file.
    db.Schema.scrub()

    printDdlToFile(schemaFile)
  }

  /**Prints the database schema to the provided file. */
  private def printDdlToFile(file: File) {
    file.delete()

    Utils.closing(new PrintWriter(file)) {
      writer =>
        db.Schema.printDdl(writer)
    }
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

  /**Location for log of the database schema SQL */
  def schemaFile = TempFile.named("schema.sql")
}