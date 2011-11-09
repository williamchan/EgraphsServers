import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import sun.misc.{BASE64Encoder, BASE64Decoder}

class Base64Test extends UnitFlatSpec
with ShouldMatchers {

  it should "encode and decode Base64" in {
    val encoder = new BASE64Encoder()
    val decoder = new BASE64Decoder()

    val str = "Base64 encode this text."
    val encoded = encoder.encode(str.getBytes)
    new String(decoder.decodeBuffer(encoded)) should be(str)
  }

}