package services.graphics

import utils.EgraphsUnitTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RectangleTests extends EgraphsUnitTest {
  "A Rectangle" should "scale properly" in {
    val rect = Rectangle(50, 50, 100, 100)
    rect.scaled(0.5) should be (Rectangle(25, 25, 50, 50))
  }
}
