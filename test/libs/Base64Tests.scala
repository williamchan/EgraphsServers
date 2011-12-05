package libs

import org.scalatest.matchers.ShouldMatchers
import play.libs.Codec
import play.test.UnitFlatSpec

class Base64Tests extends UnitFlatSpec
with ShouldMatchers {

  it should "encode and decode Base64" in {
    val str = "Base64 encode this text."

    val encoded = Codec.encodeBASE64(str)
    encoded should not be (str)
    new String(Codec.decodeBASE64(encoded)) should be(str)
  }

}