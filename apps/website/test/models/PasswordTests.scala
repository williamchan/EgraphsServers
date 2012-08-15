package models

import play.data.validation.Validation
import play.libs.Codec
import utils.EgraphsUnitTest
import org.scalatest.BeforeAndAfterEach

class PasswordTests extends EgraphsUnitTest
  with BeforeAndAfterEach
{

  override def beforeEach() {
    Validation.clear()
  }

  "A Password" should "recognize the correct password" in {
    Password("derp", 0).is("derp") should be (true)
  }

  it should "reject the incorrect password" in {
    Password("herp", 0).is("derp") should be (false)
  }

  it should "respect the password regardless of how many different salts are used" in {
    for (_ <- 1 to 100) {
      Password("derp", 0).is("derp") should be (true)
    }
  }

  it should "always have 256-bit hashes and salt" in {
    for (i <- 1 to 100) {
      val password = Password("derp", 0)
      List(password.hash, password.salt).foreach { string =>
        Codec.decodeBASE64(string).length should be (32)
      }
    }
  }

  "The n-times hashing function" should "hash n times" in {
    // Set up
    import Password.hashNTimes
    import play.libs.Crypto.passwordHash
    import play.libs.Crypto.HashType.SHA256

    val password = "derp"

    // Run tests and check expectations
    hashNTimes(password, times=0) should be (password)
    hashNTimes(password, times=1) should be (passwordHash(password, SHA256))
    hashNTimes(password, times=2) should be (
      passwordHash(passwordHash(password, SHA256), SHA256)
    )
  }
}

