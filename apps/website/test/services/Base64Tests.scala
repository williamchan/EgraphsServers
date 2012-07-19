package services

import play.libs.Codec
import utils.EgraphsUnitTest

class Base64Tests extends EgraphsUnitTest {

  it should "encode and decode Base64" in {
    val str = "Base64 encode this text."

    val encoded = Codec.encodeBASE64(str)
    encoded should not be (str)
    new String(Codec.decodeBASE64(encoded)) should be(str)
  }

}