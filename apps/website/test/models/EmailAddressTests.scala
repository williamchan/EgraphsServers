package models

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.libs.json._
import utils._

@RunWith(classOf[JUnitRunner])
class EmailAddressTests extends EgraphsUnitTest {
  "isValid" should "be true for good email address" in {
    EmailAddress.isValid("myyk@likesholdinghands.com") should be(true)
  }

  it should "be false for bad email address" in {
    EmailAddress.isValid("balls") should be(false)
  }

  "json" should "be able to transform an email to Json value" in {
    Json.toJson(EmailAddress("drlobotomy@egraphs.com")) should be(JsString("drlobotomy@egraphs.com"))
  }

  it should "be able to transform a Json value that is a valid email to a EmailAddress" in {
    val json = Json.toJson(EmailAddress("poop_stains@egraphs.com"))
    json.as[EmailAddress] should be(EmailAddress("poop_stains@egraphs.com"))
  }

  it should "be not able to transform a Json value that is an invalid email to a EmailAddress" in {
    val json = Json.toJson(EmailAddress("not_an_email"))
    intercept[JsResultException] {
      json.as[EmailAddress]
    }
  }
}