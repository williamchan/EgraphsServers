import javax.persistence.Entity
import models.{UserQueryOn, User}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach}
import play.db.jpa.QueryOn
import play.test.{Fixtures, UnitFlatSpec}

class UserTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
{

  // Test collaborators
  @Entity class TestUser extends User
  object TestUser extends QueryOn[User] with UserQueryOn[User]

  val sampleName  = "Derpy Jones"
  val sampleEmail = "herp@derp.com"

  // Test fixture
  override def beforeEach() {
    Fixtures.deleteDatabase()
  }

  //
  // Begin tests
  //
  "A User" should "instantiate with null e-mail and empty name by default" in {
    new TestUser().name should be (None)
    new TestUser().email should be ("")
  }

  it should "receive set properties correctly" in {
    // Set up
    val user = new TestUser()

    // Run test
    user.email = sampleEmail
    user.name  = sampleName

    // Check expectations
    user.email should be (sampleEmail)
    user.name should be (Some(sampleName))
  }

  it should "store and recall both e-mail and name" in {
    // Set up
    val storedUser = makeAndSaveUser(name=sampleName, email=sampleEmail)

    // Run test
    val maybeRecalledUser = TestUser.findById(storedUser.id)

    // Check expectations
    maybeRecalledUser should not be (None)

    val recalledUser = maybeRecalledUser.get
    recalledUser.getId should be (storedUser.getId())
    recalledUser.email should be (sampleEmail)
    recalledUser.name should be (Some(sampleName))
  }
  
  it should "be discoverable by the correct e-mail address, not by the incorrect" in {
    // Run test
    makeAndSaveUser(name=sampleName, email=sampleEmail).save()

    // Check expectations
    TestUser.findByEmail(sampleEmail) should not be (None)
    TestUser.findByEmail("wrong@email.com") should be (None)
  }

  def makeAndSaveUser(email: String, name: String): User = {
    val user = new TestUser
    user.name = name
    user.email = email

    user.save()
    user
  }
}