package controllers

import play.mvc._
import java.io.File
import libs.Blobs.Conversions._
import math.BigDecimal._
import play.Play
import models.{VoiceSample, Account, Celebrity}
import libs.{SampleRateConverter, Blobs}
import play.libs.Codec
import services.signature.XyzmoBiometricServices

object Application extends Controller {

  import views.Application._

  def index = {
    html.index()
  }
}

object test extends Controller
with DBTransaction {


  def resetAlphaState(): String = {
    db.Schema.scrub()
    Blobs.scrub()

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
    ).withPassword("herp").right.get.save()

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

    ""

    //    val path: File = new File("test/files")
    //    val source: File = new File(path, "voice_from_ipad.wav")
    //    val target: File = new File(path, "voice_from_ipad_8khz.wav")
    //    SampleRateConverter.convert(8000f, source, target)


    //    val voiceSample = VoiceSample.findById(1L).get
    //    val blobLocation = VoiceSample.getWavUrl(voiceSample.id)
    //    println("blobLocation = " + blobLocation)
    //
    //    val wavBinary = Blobs.get(blobLocation).get.asByteArray
    //    println("mark 0")
    //    val wavBinary_downSampled: Array[Byte] = SampleRateConverter.convert(8000f, wavBinary)
    //    println("mark 1")
    //    println(Codec.encodeBASE64(wavBinary_downSampled))
  }

}
