package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.data.validation.Validation
import play.test.UnitFlatSpec
import play.libs.Codec

class PasswordTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
{

  override def afterEach() {
    Validation.clear()
  }

  "A Password" should "recognize the correct password" in {
    Password("herp", 0).is("herp") should be (true)
  }

  it should "reject the incorrect password" in {
    Password("herp", 0).is("derp") should be (false)
  }

  it should "respect the password regardless of how many different salts are used" in {
    for (i <- 1 to 100) {
      Password("herp", 0).is("herp") should be (true)
    }
  }

  it should "always have 256-bit hashes and salt" in {
    for (i <- 1 to 100) {
      val password = Password("herp", 0)
      List(password.hash, password.salt).foreach { string =>
        Codec.decodeBASE64(string).length should be (32)
      }
    }
  }

  "The n-times hashing function" should "hash n times" in {
    // Set up
    import Password.hashNTimes
    import libs.Crypto.passwordHash
    import libs.Crypto.HashType.SHA256

    val password = "herp"

    // Run tests and check expectations
    hashNTimes(password, times=0) should be (password)
    hashNTimes(password, times=1) should be (passwordHash(password, SHA256))
    hashNTimes(password, times=2) should be (
      passwordHash(passwordHash(password, SHA256), SHA256)
    )
  }
}

