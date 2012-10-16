package egraphs.playutils

import org.apache.commons.codec.binary.Base64.{encodeBase64, decodeBase64}
import java.net.URLEncoder

object Encodings {
  object Base64 {
    def encode(bytes: Array[Byte]): String = {
      new String(encodeBase64(bytes))
    }

    def decode(toDecode: String): Array[Byte] = {
      decodeBase64(toDecode.getBytes("utf-8"))
    }
  }

  object URL {
    def encode(toEncode: String): String = {
      URLEncoder.encode(toEncode, "UTF-8")
    }
  }
}