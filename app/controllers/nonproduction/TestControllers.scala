package controllers.nonproduction

import play.mvc._
import services.blobs.Blobs.Conversions._
import math.BigDecimal._
import play.Play
import services.AppConfig
import services.http.DBTransaction
import services.blobs.Blobs
import services.db.Schema
import models._

object TestControllers extends Controller
with DBTransaction {
  val blobs = AppConfig.instance[Blobs]
  val schema = AppConfig.instance[Schema]

  def getHardwiredEgraphPage() = {
    val testFrame = LandscapeEgraphFrame

    views.Application.html.egraph(
      signerName="Herp Derpson",
      recipientName="Derp Herpson",
      frameCssClass=testFrame.cssClass,
      frameLayoutColumns=testFrame.cssFrameColumnClasses,
      productIconUrl="/Herp-Derp",
      storyTitle="The story",
      storyLayoutColumns=testFrame.cssStoryColumnClasses,
      storyBody="""
         Once upon a time in a galaxy far far away <a href="/">STAR WARS</a>.
         It was a fight to the finish between foes, mortal enemies that were
         born of the same womb. I just...I just don't have anything more to say.
         It's so late...
         """,
      audioUrl="http://freshly-ground.com/data/audio/sm2/Adrian%20Glynn%20-%20Blue%20Belle%20Lament.mp3",
      signedImageUrl="/SomeImageSample",
      signedOnDate="February 22, 2012"
    )
  }

  def resetAlphaState(): String = {
    val applicationMode = play.Play.configuration.get("application.mode")
    if (applicationMode != "dev") {
      throw new IllegalStateException("Cannot reset-alpha-state unless in dev mode")
    }

    schema.scrub()
    blobs.scrub()

    createCelebrity("Erem", "Boto", "erem@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Andrew", "Smith", "andrew@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("David", "Auld", "david@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Eric", "Feeny", "eric@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Will", "Chan", "will@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Zach", "Apter", "zachapter@gmail.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Brian", "Auld", "bauld@raysbaseball.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Michael", "Kalt", "mkalt@raysbaseball.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Matt", "Silverman", "msilverman@raysbaseball.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("Gunter", "Gebauhr", "gunter@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
    createCelebrity("J", "Cohn", "j@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)

    "Alpha Testers created!"
  }

  private def createCelebrity(firstName: String, lastName: String, email: String, enrollmentStatus: EnrollmentStatus = EnrollmentStatus.NotEnrolled) {
    println("Creating Celebrity " + email + " ...")

    val celebrity = Celebrity(
      firstName = Some(firstName),
      lastName = Some(lastName),
      publicName = Some(firstName + " " + lastName),
      description = Some("Today's Sriracha is tomorrow's salsa."),
      enrollmentStatusValue = enrollmentStatus.value
    ).save()

    Account(email = email,
      celebrityId = Some(celebrity.id)
    ).withPassword("derp").right.get.save()

    celebrity.newProduct.copy(
      priceInCurrency = 50,
      name = celebrity.publicName.get + "'s Product A",
      description = "Tyson 15"
    ).save().withPhoto(Play.getFile("test/files/longoria/product-2.jpg")).save()

    celebrity.newProduct.copy(
      priceInCurrency = 100,
      name = celebrity.publicName.get + "'s Product B",
      description = "Help me... help YOU..."
    ).save().withPhoto(Play.getFile("test/files/kapler/product-1.jpg")).save()

  }

  def script = {
//    println("System getProperty javax.net.ssl.trustStore = " + System.getProperty("javax.net.ssl.trustStore"))
//    val startEnrollmentRequest = services.voice.VBGDevRandomNumberBiometricServices.sendStartEnrollmentRequest("pls", false)
//    println(startEnrollmentRequest.getResponseValue("errorcode"))
//    sjson.json.Serializer.SJSON.toJSON(Map("bees.api.name" -> Play.configuration.getProperty("bees.api.name")))
    new jobs.EnrollmentBatchJob().now()
  }
}
