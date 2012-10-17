package models

import utils.EgraphsUnitTest
import egraphs.playutils.Encodings.Base64
import org.scalatest.BeforeAndAfterEach

class PasswordTests extends EgraphsUnitTest {

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
        Base64.decode(string).length should be (32)
      }
    }
  }

  "The n-times hashing function" should "hash n times" in {
    // Set up
    import Password.hashNTimes
    import services.crypto.Crypto.SHA256

    val password = "derp"

    // Run tests and check expectations
    hashNTimes(password, times=0) should be (password)
    hashNTimes(password, times=1) should be (SHA256.hash(password))
    hashNTimes(password, times=2) should be (SHA256.hash(SHA256.hash(password)))    
  }
}

