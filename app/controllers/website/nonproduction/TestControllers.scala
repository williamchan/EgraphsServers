package controllers.website.nonproduction

import play.mvc._
import math.BigDecimal._
import play.Play
import services.AppConfig
import services.blobs.Blobs
import services.db.Schema
import models._
import services.http.ControllerMethod
import services.logging.Logging
import java.text.SimpleDateFormat
import org.joda.time.DateTime
import javax.imageio.ImageIO

object TestControllers extends Controller with Logging {
  val controllerMethod = AppConfig.instance[ControllerMethod]
  val blobs = AppConfig.instance[Blobs]
  val schema = AppConfig.instance[Schema]
  val accountServices = AppConfig.instance[AccountServices]
  val administratorServices = AppConfig.instance[AdministratorServices]
  val celebrityServices = AppConfig.instance[CelebrityServices]
  val customerServices = AppConfig.instance[CustomerServices]

  private lazy val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  private lazy val today = DateTime.now().toLocalDate.toDate
  private lazy val future = dateFormat.parse("2020-01-01")

  def memcached() = controllerMethod() {
    import play.cache.Cache

    Cache.set("reservation", "asdf", "1s")
//    Thread.sleep(990)
    val x: Option[String] = Cache.get[String]("reservation")
    println("x " + x)

//    Cache.safeDelete("reservation")
//    val y: Option[String] = Cache.get[String]("reservation")
//    println("y " + y)
  }

  def getHardwiredEgraphPage() = controllerMethod() {
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

  def resetAlphaState() = controllerMethod() {
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
    createCelebrity("Mike", "Ginal", "mike@egraphs.com", enrollmentStatus = EnrollmentStatus.NotEnrolled)
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

    val administrator = Administrator().save()

    Account(email = email,
      celebrityId = Some(celebrity.id),
      administratorId = Some(administrator.id)
    ).withPassword("egraphsa").right.get.save()

    val product1 = celebrity.newProduct.copy(
      priceInCurrency = 50,
      name = celebrity.publicName.get + "'s Product A",
      description = "Tyson 15"
    ).saveWithImageAssets(image = Some(ImageIO.read(Play.getFile("test/files/longoria/product-2.jpg"))), icon = None)

    val product2 = celebrity.newProduct.copy(
      priceInCurrency = 100,
      name = celebrity.publicName.get + "'s Product B",
      description = "Help me... help YOU..."
    ).saveWithImageAssets(image = Some(ImageIO.read(Play.getFile("test/files/kapler/product-1.jpg"))), icon = None)

    val inventoryBatch = InventoryBatch(celebrityId = celebrity.id, numInventory = 100, startDate = today, endDate = future).save()
    inventoryBatch.products.associate(product1)
    inventoryBatch.products.associate(product2)
  }

  def logStuffThenThrowException() = controllerMethod() {
    log("I'm pretty happy to be alive")
    log("Just chugging along with this method")
    log("Can't wait to send a nice webpage down to the client...")
    log("Wait, is that a bear???")
    log("Oh no! It's a bear!")
    log("Don't kill me please...I don't, I don't want to--")

    val illegalE = new IllegalArgumentException("Bear")
    throw new RuntimeException("Process was mauled by a bear", illegalE)
  }

  def createTestOrders(msg: String) = controllerMethod() {
    var results = List.empty[String]

    val celebrityEmails = List(
      "erem@egraphs.com",
      "andrew@egraphs.com",
      "david@egraphs.com",
      "eric@egraphs.com",
      "will@egraphs.com",
      "zachapter@gmail.com",
      "bauld@raysbaseball.com",
      "mkalt@raysbaseball.com",
      "msilverman@raysbaseball.com",
      "gabe@egraphs.com",
      "mike@egraphs.com",
      "j@egraphs.com",
      "kate@egraphs.com")

    for (email <- celebrityEmails) {
      val account = accountServices.accountStore.findByEmail(email)
      if (account.isEmpty) {
        results = ("\"Unable to find Account " + email + "\"") :: results
      } else {
        val celebrity = celebrityServices.store.findById(account.get.celebrityId.get)
        results = (orderFromCelebrity(celebrity = celebrity.get, celebrityEmail = email, msg = msg, numOrders = 5)) :: results
      }
    }

    Json("[" + results.mkString(",") + "]")
  }

  private def orderFromCelebrity(celebrity: Celebrity, celebrityEmail: String, msg: String, numOrders: Int): String = {
    val product = celebrity.productsInActiveInventoryBatches().headOption
    if (product.isEmpty) {
      "\"No products found for celebrity " + celebrity.publicName + "\""
    } else {
      val buyer = customerServices.customerStore.findOrCreateByEmail(celebrityEmail, celebrity.publicName.get)
      for (i <- 0 until numOrders) {
        buyer.buy(product.get, buyer, recipientName=celebrityEmail, messageToCelebrity=Some(msg), requestedMessage=Some(msg))
          .copy(reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue)
          .save()
      }
      "\"Created " + numOrders + " orders for celebrity " + celebrity.publicName.get + "\""
    }
  }
}