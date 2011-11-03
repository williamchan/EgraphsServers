import javax.persistence.Entity
import models.CreatedUpdated
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import play.db.jpa.Model
import play.test.{Fixtures, UnitFlatSpec}

class CreatedUpdatedTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with DateShouldMatchers
{
  //
  // Test collaborators
  //
  @Entity class TestCreatedUpdated(var name: String="Derpy") extends Model
    with CreatedUpdated

  // Test fixture
  override def beforeEach() {
    Fixtures.deleteDatabase()
  }

  //
  // Begin tests
  //
  "An object mixing in CreatedUpdated" should "instantiate with null values" in {
    assert(new TestCreatedUpdated().created == null)
    assert(new TestCreatedUpdated().updated == null)
  }

  it should "generate created and updated values at save, which should both be right now" in {
    // Set up
    val testEntity = new TestCreatedUpdated

    // Run test
    testEntity.save()

    // Check expectations
    testEntity.created should be (aboutNow)
    assert(testEntity.created == testEntity.updated)
  }

  it should "update the updated field if re-saved after a few milliseconds" in {
    // Set up
    val testEntity = new TestCreatedUpdated

    testEntity.save()

    val initialCreated = testEntity.created
    val initialUpdated = testEntity.updated

    // Run test
    Thread.sleep(100)

    // alter some data so it will actually update
    testEntity.name = "Derpy Jones"
    testEntity.save()

    val subsequentCreated = testEntity.created
    val subsequentUpdated = testEntity.updated

    subsequentCreated.getTime should be (initialCreated.getTime)
    subsequentUpdated.getTime should be ((initialUpdated.getTime + 100) plusOrMinus 20)
  }
}