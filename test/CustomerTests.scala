import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec

class CustomerTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with DateShouldMatchers
{

  it should "herp and derp" in {
    fail("derpity")
  }

}