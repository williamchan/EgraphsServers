import javax.persistence.Entity
import models.{Credential, Password, PasswordProtected}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.data.validation.Validation
import play.db.jpa.{QueryOn, Model}
import play.test.UnitFlatSpec
import scala.collection.JavaConversions._
import play.libs.Codec

class CredentialTest extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
{

  override def afterEach() {
    Validation.clear()
    db.DB.scrub
  }

  "A Credential" should "start unprotected" in {
    Credential().password should be (None)
  }

  it should "become protected once a password is set" in {
    credentialWithPassword("herp").password should not be (None)
  }

  it should "have different hashes and salts when the same password is set twice" in {
    val credential = credentialWithPassword("herp")
    val firstPassword = credential.password.get

    val password = credential.withPassword("herp").right.get.password.get

    firstPassword.is("herp") should be (true)
    password.is("herp") should be (true)
    password.hash should not be (firstPassword.hash)
    password.salt should not be (firstPassword.salt)
  }

  it should "store and retrieve correctly" in {
    // Set up
    val stored = credentialWithPassword("herp")
    val storedPassword = stored.password.get

    // Run test
    val saved = Credential.save(stored)
    val maybeRecalled = Credential.byId(saved.id)

    // Check expectations
    maybeRecalled should not be (None)

    val recalled = maybeRecalled.get
    val recalledPassword = recalled.password.get
    recalled.id should be (stored.id)
    recalledPassword should be (storedPassword)
    recalledPassword.is("herp") should be (true)
  }

  it should "fail validation for password lengths shorter than 4 characters" in {
    for (password <- List("", "123")) {
      val errorOrCredential = Credential().withPassword(password)
      errorOrCredential.isLeft should be (true)

      Validation.errors should have length (1)
      Validation.errors.head.getKey should be ("password")
      Validation.clear()
    }
  }

  it should "pass validation for password lengths 4 characters and longer" in {
    Credential().withPassword("herp").isRight should be (true)

    Validation.errors should have length (0)
  }

  def credentialWithPassword(password: String): Credential = {
    Credential.save(Credential().withPassword(password).right.get)
  }
}