package services

import utils.EgraphsUnitTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SerializationTests extends EgraphsUnitTest {
  import services.Utils.makeOptionalFieldMap

  "makeOptionalFieldMap" should "return an empty map if no options have a value" in {
    val map = makeOptionalFieldMap(
      List(
        "one" -> None,
        "two" -> None,
        "three" -> None
      )
    )

    map.size should be (0)
  }

  it should "contain the existing values" in {
    val map = makeOptionalFieldMap(
      List(
        "one" -> Some(1),
        "two" -> None,
        "three" -> Some(3)
      )
    )

    map should be (Map("one" -> 1, "three" -> 3))
  }
}