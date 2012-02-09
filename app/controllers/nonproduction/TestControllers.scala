package controllers.nonproduction

import play.mvc._
import services.blobs.Blobs.Conversions._
import math.BigDecimal._
import play.Play
import models.{Account, Celebrity}
import services.AppConfig
import services.http.DBTransaction
import services.blobs.Blobs
import services.db.Schema

object TestControllers extends Controller
with DBTransaction {
  val blobs = AppConfig.instance[Blobs]
  val schema = AppConfig.instance[Schema]

  def resetAlphaState(): String = {
    schema.scrub()
    blobs.scrub()

    createCelebrity("Erem", "Boto", "erem@egraphs.com")
    createCelebrity("Andrew", "Smith", "andrew@egraphs.com")
    createCelebrity("David", "Auld", "david@egraphs.com")
    createCelebrity("Eric", "Feeny", "eric@egraphs.com")
    createCelebrity("Will", "Chan", "will@egraphs.com")
    createCelebrity("Zach", "Apter", "zachapter@gmail.com")
    createCelebrity("Brian", "Auld", "bauld@raysbaseball.com")
    createCelebrity("Michael", "Kalt", "mkalt@raysbaseball.com")
    createCelebrity("Matt", "Silverman", "msilverman@raysbaseball.com")
    createCelebrity("Gabe", "Kapler", "gabe@egraphs.com")

    "Alpha Testers created!"
  }

  private def createCelebrity(firstName: String, lastName: String, email: String) {
    println("Creating Celebrity " + email + " ...")

    val celebrity = Celebrity(
      firstName = Some(firstName),
      lastName = Some(lastName),
      publicName = Some("Alpha " + firstName),
      description = Some("Today's Sriracha is tomorrow's salsa.")
    ).save()

    Account(email = email,
      celebrityId = Some(celebrity.id)
    ).withPassword("derp").right.get.save()

    celebrity.newProduct.copy(
      priceInCurrency = 50,
      name = firstName + "'s Alpha Product A",
      description = "Tyson 15"
    ).save().withPhoto(Play.getFile("test/files/longoria/product-2.jpg")).save()

    celebrity.newProduct.copy(
      priceInCurrency = 100,
      name = firstName + "'s Alpha Product B",
      description = "Help me... help YOU..."
    ).save().withPhoto(Play.getFile("test/files/kapler/product-1.jpg")).save()

  }

  def script() {
    println("System getProperty javax.net.ssl.trustStore = " + System.getProperty("javax.net.ssl.trustStore"))
    import services.voice.VBGBiometricServices
    val startEnrollmentRequest = VBGBiometricServices.sendStartEnrollmentRequest("pls", false)
    println(startEnrollmentRequest.getResponseValue(VBGBiometricServices._errorCode))
  }
}
