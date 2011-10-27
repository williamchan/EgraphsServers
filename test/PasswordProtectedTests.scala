import javax.persistence.Entity
import models.PasswordProtected
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
    new TestPasswordProtected().hasPassword should be (false)
  }

  it should "become protected once a password is set" in {
    // Set up
    val entity = new TestPasswordProtected()

    // Run test
    entity.setPassword("herp")

    // Check expectations
    entity.hasPassword should be (true)
  }

  it should "recognize the correct password" in {
    entityWithPassword("herp").passwordIs("herp") should be (true)
  }

  it should "reject the incorrect password" in {
    entityWithPassword("herp").passwordIs("derp") should be (false)
  }

  it should "have different hashes and salts when the same password is set twice" in {
    val entity = entityWithPassword("herp")

    val firstHash = entity.passwordHash
    val firstSalt = entity.passwordSalt

    entity.setPassword("herp")

    entity.passwordHash should not be (firstHash)
    entity.passwordSalt should not be (firstSalt)
  }

  it should "respect the password regardless of how many different salts are used" in {
    val entity = new TestPasswordProtected()

    for (i <- 1 to 100) {
      entity.setPassword("herp")
      entity.passwordIs("herp") should be (true)
    }
  }

  it should "always have 256-bit hashes and salt" in {
    for (i <- 1 to 100) {
      val entity = entityWithPassword("herp")
      List(entity.passwordHash, entity.passwordSalt).foreach { string =>
        Codec.decodeBASE64(string).length should be (32)
      }
    }
  }

  it should "store and retrieve correctly" in {
    // Set up
    val stored = entityWithPassword("herp")

    // Run test
    stored.save()
    val maybeRecalled = TestPasswordProtected.findById(stored.getId())

    // Check expectations
    maybeRecalled should not be (None)

    val recalled = maybeRecalled.get
    recalled.getId should be (stored.getId())
    recalled.passwordHash should be (stored.passwordHash)
    recalled.passwordSalt should be (stored.passwordSalt)
    stored.passwordIs("herp") should be (true)
    recalled.passwordIs("herp") should be (true)
  }

  it should "fail validation for password lengths shorter than 4 characters" in {
    // Set up
    import scala.collection.JavaConversions._

    val entity = new TestPasswordProtected()

    // Run with empty string and check expectations
    for (password <- List("", "123")) {
      entity.setPassword(password).ok should be (false)
      entity.hasPassword should be (false)

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

  "The n-times hashing function" should "hash n times" in {
    // Set up
    import PasswordProtected.hash
    import libs.Crypto.passwordHash
    import libs.Crypto.HashType.SHA256

    val password = "herp"

    // Run tests and check expectations
    hash(password, times=0) should be (password)
    hash(password, times=1) should be (passwordHash(password, SHA256))
    hash(password, times=2) should be (
      passwordHash(passwordHash(password, SHA256), SHA256)
    )
  }

  def entityWithPassword(password: String): TestPasswordProtected = {
    val entity = new TestPasswordProtected()
    entity.setPassword(password)

    entity
  }

}