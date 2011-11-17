package libs

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers


class BezierCubicTest extends UnitFlatSpec
with ShouldMatchers {

  it should "calculate distance" in {
    BezierCubic.distance(1, 1, 4, 5) should be(5)
  }

  it should "calcXCoord" in {
    val bezier = BezierCubic(
      0d, 0d,
      0d, 4d,
      4d, 4d,
      4d, 0d
    )
    bezier.calcXCoord(0) should be(0)
    bezier.calcXCoord(.25) should be(0.625)
    bezier.calcXCoord(.5) should be(2)
    bezier.calcXCoord(.75) should be(3.375)
    bezier.calcXCoord(1) should be(4)
  }

  it should "calcYCoord" in {
    val bezier = BezierCubic(
      0d, 0d,
      0d, 4d,
      4d, 4d,
      4d, 0d
    )
    bezier.calcYCoord(0) should be(0)
    bezier.calcYCoord(.25) should be(2.25)
    bezier.calcYCoord(.5) should be(3)
    bezier.calcYCoord(.75) should be(2.25)
    bezier.calcYCoord(1) should be(0)
  }

  it should "calculateTClosestToC0" in {
    val bezier = BezierCubic(
      0d, 0d,
      0d, 4d,
      4d, 4d,
      4d, 0d
    )
//    bezier.calculateTClosestToC0() should be(0.3333333333333333)
//    bezier.calculateTClosestToC0() should be(0.33 plusOrMinus 0.1)
  }

}