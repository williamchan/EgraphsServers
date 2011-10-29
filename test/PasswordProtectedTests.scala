import javax.persistence.Entity
import models.{Password, PasswordProtected}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.data.validation.Validation
import play.db.jpa.{QueryOn, Model}
import play.test.UnitFlatSpec
import play.libs.Codec

class PasswordProtectedTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
{

  override def afterEach() {
    Validation.clear()
  }

  // Test collaborators
  @Entity class TestPasswordProtected(var name: String="Derpy") extends Model
    with PasswordProtected

  object TestPasswordProtected extends QueryOn[TestPasswordProtected]

  "A PasswordProtected entity" should "start unprotected" in {
    new TestPasswordProtected().password should be (None)
  }

  it should "become protected once a password is set" in {
    // Set up
    val entity = new TestPasswordProtected()

    // Run test
    entity.setPassword("herp")

    // Check expectations
    entity.password should not be (None)
  }

  it should "have different hashes and salts when the same password is set twice" in {
    val entity = entityWithPassword("herp")

    val firstPassword = entity.password.get

    entity.setPassword("herp")

    val password = entity.password.get

    firstPassword.is("herp") should be (true)
    password.is("herp") should be (true)
    password.hash should not be (firstPassword.hash)
    password.salt should not be (firstPassword.salt)
  }

  it should "store and retrieve correctly" in {
    // Set up
    val stored = entityWithPassword("herp")
    val storedPassword = stored.password.get
    // Run test
    stored.save()
    val maybeRecalled = TestPasswordProtected.findById(stored.getId())

    // Check expectations
    maybeRecalled should not be (None)

    val recalled = maybeRecalled.get
    val recalledPassword = recalled.password.get
    recalled.getId should be (stored.getId())
    recalledPassword should be (storedPassword)
    recalledPassword.is("herp") should be (true)
  }

  it should "fail validation for password lengths shorter than 4 characters" in {
    // Set up
    import scala.collection.JavaConversions._

    val entity = new TestPasswordProtected()

    // Run with empty string and check expectations
    for (password <- List("", "123")) {
      entity.setPassword(password).ok should be (false)
      entity.password should be (None)

      Validation.errors should have length (1)
      Validation.errors.head.getKey should be ("password")

      Validation.clear()
    }
  }

  it should "pass validation for password lengths 4 characters and longer" in {
    val entity = new TestPasswordProtected()
    entity.setPassword("herp").ok should be (true)

    Validation.errors should have length (0)
  }

  "A Password" should "recognize the correct password" in {
    Password("herp", 0).is("herp") should be (true)
  }

  it should "reject the incorrect password" in {
    Password("herp", 0).is("derp") should be (false)
  }

  it should "respect the password regardless of how many different salts are used" in {
    for (i <- 1 to 100) {
      Password("herp", 0).is("herp") should be (true)
    }
  }

  it should "always have 256-bit hashes and salt" in {
    for (i <- 1 to 100) {
      val password = Password("herp", 0)
      List(password.hash, password.salt).foreach { string =>
        Codec.decodeBASE64(string).length should be (32)
      }
    }
  }

  "The n-times hashing function" should "hash n times" in {
    // Set up
    import Password.hashNTimes
    import libs.Crypto.passwordHash
    import libs.Crypto.HashType.SHA256

    val password = "herp"

    // Run tests and check expectations
    hashNTimes(password, times=0) should be (password)
    hashNTimes(password, times=1) should be (passwordHash(password, SHA256))
    hashNTimes(password, times=2) should be (
      passwordHash(passwordHash(password, SHA256), SHA256)
    )
  }

  def entityWithPassword(password: String): TestPasswordProtected = {
    val entity = new TestPasswordProtected()
    entity.setPassword(password)

    entity
  }

}