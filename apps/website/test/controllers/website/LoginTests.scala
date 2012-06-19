package controllers.website

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.{ClearsDatabaseAndValidationBefore, EgraphsUnitTest, TestWebsiteControllers, TestData}
import services.http.forms.{Form, CustomerLoginFormFactory, CustomerLoginForm}
import CustomerLoginForm.Fields
import play.mvc.results.Redirect
import controllers.WebsiteControllers


class LoginFunctionalTests extends EgraphsFunctionalTest {
  @Test
  def testLoginEndpointServesCorrectRoute() {
    val response = POST("/login", getPostStrParams(email = "", password = ""))
    assertStatus(302, response)

    val flashCookie = getPlayFlashCookie(response)
    assertTrue(flashCookie.contains("CustomerLoginForm:true"))
    assertTrue(flashCookie.contains(Fields.Email.name + ":"))
    assertTrue(flashCookie.contains(Fields.Password.name + ":"))
  }

  private def getPostStrParams(email: String, password: String): Map[String, String] = {
    Map[String, String](Fields.Email.name -> email, Fields.Password.name -> password)
  }
}


class LoginUnitTests extends EgraphsUnitTest with ClearsDatabaseAndValidationBefore {
  import Form.Conversions._
  private val db = AppConfig.instance[DBSession]

  "postLogin" should "redirect to login with form information when passwords don't match" in {
    val controller = testControllers(email="idontexist@egraphs.com", password=TestData.defaultPassword)

    controller.postLogin() match {
      case redirect: Redirect => db.connected(TransactionSerializable) {
        redirect.url should be ("/login")
        val customerFormOption = AppConfig.instance[CustomerLoginFormFactory].read(controller.flash.asFormReadable)

        customerFormOption match {
          case Some(form) =>
            form.email.value should be (Some("idontexist@egraphs.com"))
            form.password.value should be (Some(TestData.defaultPassword))
            form.fieldInspecificErrors.map(_.description).headOption.getOrElse("") should be (
              CustomerLoginForm.badCredentialsMessage
            )

          case None =>
            fail("There should have been serialized form in the flash")
        }
      }

      case _ =>
        fail("login should have produced a Redirect")
    }
  }

  "postLogin" should "add customer ID to session when passwords do match" in {
    // Set up
    val password = "this is teh password"
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withPassword(password).right.get.save()
    }

    val controller = testControllers(email=account.email, password=password)

    controller.postLogin()

    // Check expectations
    AppConfig.instance[CustomerLoginFormFactory].read(controller.flash.asFormReadable) should be (
      None
    )

    controller.session.get(WebsiteControllers.customerIdKey) should be (account.customerId.get.toString)
  }

  private def testControllers(email: String, password:String): TestWebsiteControllers = {
    val controllers = TestData.newControllers
    controllers.params.put(Fields.Email.name, email)
    controllers.params.put(Fields.Password.name, password)

    controllers
  }
}