package services.crypto

import java.security.MessageDigest
import org.postgresql.util.Base64

private[crypto] trait StringHashing { this: HashAlgorithm =>

  def hash(input: String): String = {
    val messageDigest = MessageDigest.getInstance(name)
    val bytesOut = messageDigest.digest(input.getBytes)

    new String(Base64.encodeBytes(bytesOut))
  }
}
