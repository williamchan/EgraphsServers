package services.crypto

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import org.postgresql.util.Base64

object Crypto {
  val SHA256 = new HashAlgorithm("SHA-256") with StringHashing
  val MD5 = new HashAlgorithm("MD5") with StringHashing
}