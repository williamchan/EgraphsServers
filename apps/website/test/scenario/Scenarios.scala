package scenario

import services.db.Schema
import services.blobs.Blobs
import org.apache.commons.mail.SimpleEmail
import play.mvc.results.Redirect
import Blobs.Conversions._
import utils.{TestConstants, TestData}
import org.squeryl.PrimitiveTypeMode._
import models._
import controllers.WebsiteControllers
import controllers.website.PostBuyProductEndpoint.EgraphPurchaseHandler
import enums.{EgraphState, EnrollmentStatus, PublishedStatus}
import play.libs.{Codec, Mail}
import services.payment.Payment
import play.Play
import services.{Utils, AppConfig}
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import javax.imageio.ImageIO

/**
 * All scenarios supported by the API.
 */
class Scenarios extends DeclaresScenarios {
  // Helpful services
  private val blobs = AppConfig.instance[Blobs]
  private val payment = AppConfig.instance[Payment]
  private val schema = AppConfig.instance[Schema]

  // Categories of scenario
  private val adminCategory = "Admin Helpers"
  private val apiCategory = "API Helpers"
  private val celebrityPageCategory = "Celebrity Page"
  private val productPageCategory = "Product Page"
  private val orderConfirmationPageCategory = "Order Confirmation Page"
  private val egraphPageCategory = "Egraph Page"

  private val mail = AppConfig.instance[services.mail.Mail]

  private lazy val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  private lazy val today = DateTime.now().toLocalDate.toDate
  private lazy val future = dateFormat.parse("2020-01-01")

  toScenarios add Scenario(
    "Send an email to erem@egraphs.com",

    apiCategory,

    """Sends an email to erem@egraphs.com using the configured SMTP server""",

    {() =>
      val email = new SimpleEmail()

      email.setMailSession(Mail.getSession)
      email.setFrom("ehboto@gmail.com")
      email.setSubject("Hi")
      email.setMsg("This is a test mail ... :-)")
      email.addTo("erem@egraphs.com", "Erem Boto")

      mail.send(email)
    }

  )

  toScenarios add Scenario(
    "Will Chan is a celebrity",

    apiCategory,

    """
    Creates a celebrity named William 'Wizzle' Chan. His login/password are
    wchan83@egraphs.com/derp. He has a profile photo.
    """,

   {() =>
     demosetup.DemoScenarios.createAdmin()

      val celebrity = Celebrity(
        firstName=Some("William"),
        lastName=Some("Chan"),
        publicName=Some("Wizzle"),
        description=Some("Love my fans from New York to Tokyo, from Seoul to the Sudetenland. And for all you haters out there -- don't mess around. I sleep with one eye closed, the other fixed on my Vespene gas supply.")
      ).withPublishedStatus(PublishedStatus.Published).save()

      Account(email="wchan83@egraphs.com",
              celebrityId=Some(celebrity.id)
      ).withPassword(TestData.defaultPassword).right.get.save()

     celebrity.saveWithProfilePhoto(Play.getFile("./test/files/will_chan_celebrity_profile.jpg"))
   }
  )

  toScenarios add Scenario(
    "Will has two products",

    apiCategory,

    """
    Adds two products to Wizzle's product portfolio. The first costs $100 and
    is an image of him at the 2010 Starcraft 2 Championships. The second costs
    $200 and is an image of him at the 2011 King of Pweens Competition, where he
    first established dominance in the arena of Pweendom.
    """,

    {() =>
      val will = Scenarios.getWillCelebrityAccount

      val photoImage = Some(Product().defaultPhoto.renderFromMaster)
      val iconImage = Some(Product().defaultIcon.renderFromMaster)

      val product1 = will.addProduct(
        name="2010 Starcraft 2 Championships",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one.",
        priceInCurrency=10,
        image=photoImage,
        icon=iconImage,
        storyTitle="The story and the glory",
        storyText="{signer_link}{signer_name}{end_link} was born on top. He proved it to the world at the {product_link}{product_name}{end_link}. A few days afterwards he got a note from {recipient_name} on his iPad. This was his response.",
        publishedStatus = PublishedStatus.Published
      )

      val product2 = will.addProduct(
        name="2011 King of Pweens Competition",
        description="In classic form, Wizzle dominated the competition and left mouths agape.",
        priceInCurrency=70,
        storyTitle="The story and the glory",
        image=photoImage,
        icon=iconImage,
        storyText="""
           {signer_link}{signer_name}{end_link} was born on top. On {date_signed}
           he proved it to {recipient_name}.
           """,
        publishedStatus = PublishedStatus.Published
      )
      val inventoryBatch = InventoryBatch(celebrityId = will.id, numInventory = 100, startDate = today, endDate = future).save()
      inventoryBatch.products.associate(product1)
      inventoryBatch.products.associate(product2)
    }
  )

  toScenarios add Scenario(
    "Erem is a customer",

    apiCategory,

    """
    Creates a customer named Erem Boto.

    [note: He does not have any associated Account thus far...but we can create one
     programmatically later on if we want to log in as him]
    """,

    {() =>
      TestData.newSavedCustomer().copy(name="Erem Boto").save()
    }
  )

  toScenarios add Scenario(
    "Erem buys Wills two products twice each",

    apiCategory,

    """
    Creates two unfulfilled orders, one each ordered against Will's
    two products.
    """,

    {() =>
      val erem = Scenarios.getEremCustomerAccount
      val (starcraftChampionship, kingOfPweensCompetition) = Scenarios.getWillsTwoProducts

      erem.buy(starcraftChampionship, recipientName="Erem Boto", requestedMessage=Some("Happy 13th birthday, Don!"), messageToCelebrity=Some("My buddy Don is your biggest fan!")).save()
      erem.buy(kingOfPweensCompetition, recipientName="Erem Boto", requestedMessage=Some("Happy Pweenday, Don!"), messageToCelebrity=Some("Don loves everything you do!")).save()
    }
  )

  toScenarios add Scenario(
    "Will fulfills one of Erem's product orders",
    apiCategory,
    """
    Will fulfills one of Erem's product orders
    """,

    {() =>
      val (starcraftChampionship, kingOfPweensCompetition) = Scenarios.getWillsTwoProducts

      val firstOrder = from(schema.orders)(order =>
        where(order.id in List(starcraftChampionship.id, kingOfPweensCompetition.id))
        select (order)
      ).headOption.get

      firstOrder
        .newEgraph
        .withAssets(TestConstants.signingAreaSignatureStr, Some(TestConstants.signingAreaMessageStr), Codec.decodeBASE64(TestConstants.voiceStr()))
        .save()
        .withYesMaamBiometricServices
        .verifyBiometrics
        .withEgraphState(EgraphState.Published)
        .save()
    }
  )

  toScenarios add Scenario(
    "Will is faux-enrolled in biometric services",

     apiCategory,

    """
    Marks celebrity Will as enrolled in biometrics. Does not _actually_ create
    biometric profiles for him on real services.
    """,

    {() =>
      Scenarios.getWillCelebrityAccount.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()
    }
  )

  toScenarios add Scenario(
    "Will failed faux-enrollment in biometric services",

     apiCategory,

    """
    Marks celebrity Will as having failed enrollment in biometrics. Does not _actually_
    attempt to make biometric profiles for him on real services.
    """,

    {() =>
      Scenarios.getWillCelebrityAccount.withEnrollmentStatus(EnrollmentStatus.FailedEnrollment).save()
    }
  )

  toScenarios add Scenario(
    "A public image is on the blobstore",

    apiCategory,

    """
    Adds an image with the key "a/b/derp.jpg" key to the blobstore. It
    should be accessible via /test/files/a/b/derp.jpg
    """,

    {() =>
      import Blobs.Conversions._
      blobs.put("a/b/derp.jpg", Play.getFile("./test/files/derp.jpg"))
    }
  )

  toScenarios add Scenario(
    "2 products",

    celebrityPageCategory,

    """ Opens up Wizzle's celebrity page with two products """,

    {() =>
      Scenario.clearAll()

      Scenario.play(
        "Will-Chan-is-a-celebrity",
        "Will-has-two-products"
      )

      redirectToWizzle
    }
  )

  toScenarios add Scenario(
    "1 product",

    celebrityPageCategory,

    """ Opens up Wizzle's celebrity page with one product """,

    {() =>
      Scenario.clearAll()

      Scenario.play("Will-Chan-is-a-celebrity")

      val will = Scenarios.getWillCelebrityAccount
      createProduct(celebrity = will,
        priceInCurrency=100,
        name="2010 Starcraft 2 Championships",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      )

      redirectToWizzle
    }
  )

  toScenarios add Scenario(
    "5 products",

    celebrityPageCategory,

    """ Opens up Wizzle's celebrity page with five products """,

    {() =>
      Scenario.clearAll()

      Scenario.play("Will-Chan-is-a-celebrity")

      val will = Scenarios.getWillCelebrityAccount

      createProduct(celebrity = will,
        priceInCurrency=100,
        name="2010 Starcraft 2 Championships",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      )

      createProduct(celebrity = will,
        priceInCurrency=90,
        name="2012 Platinum League Victory",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      )

      createProduct(celebrity = will,
        priceInCurrency=89.99,
        name="2001 Senior Yearbook Photo",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      )

      createProduct(celebrity = will,
        priceInCurrency=100.50,
        name="Bi-Annual World Series of Magic: The Gathering",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      )

      createProduct(celebrity = will,
        priceInCurrency=120.00,
        name="2200AD Undead League Starcraft II Championship",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      )

      redirectToWizzle
    }
  )

  toScenarios add Scenario(
    "0 products",

    celebrityPageCategory,

    """ Opens up Wizzle's celebrity page with no products """,

    {() =>
      Scenario.clearAll()

      Scenario.play("Will-Chan-is-a-celebrity")

      redirectToWizzle
    }
  )

  toScenarios add Scenario(
    "Accessed from Celebrity Page or from URL",

    productPageCategory,

    """Displays the product page as if it had been accessed via the link on the Celebrity page.""",

    {() =>
      Scenario.clearAll()
      createAndSaveStarcraftProduct

      redirectToStarcraftProduct
    }
  )

  toScenarios add Scenario(
    "From a new customer",

    orderConfirmationPageCategory,

    """
    Processes an order with a (hopefully) successful set of new credentials, pops you into
    the order confirmation page
    """,

    {() =>
      Scenario.clearAll()
      val product = createAndSaveStarcraftProduct
      val celebrity = Scenarios.getWillCelebrityAccount

      EgraphPurchaseHandler(
        recipientName = "Erem Boto",
        recipientEmail = "ehboto@gmail.com",
        buyerName = "Rooster McGillycuddy",
        buyerEmail = "rooster@egraphs.com",
        stripeTokenId = payment.testToken().id,
        desiredText = Some("Happy 29th birthday, Erem!"),
        personalNote = Some("I'm your biggest fan!"),
        celebrity = celebrity,
        product = product
      ).execute()
    }
  )

  toScenarios add Scenario(
    "Valid, ordered landscape Egraph",

    egraphPageCategory,

    """
    Creates a signed Egraph and views its page
    """,

    {() =>
      Scenario.clearAll()

      Scenario.play(
        "Will-Chan-is-a-celebrity",
        "Will-has-two-products",
        "Erem-is-a-customer",
        "Erem-buys-Wills-two-products-twice-each",
        "Deliver-All-Orders-to-Celebrities",
        "Will-fulfills-one-of-Erems-product-orders"
      )

      new Redirect(
        WebsiteControllers.lookupGetEgraph(2).url
      )
    }
  )

  toScenarios add Scenario(
    "Create Admin",
    adminCategory,
    """
    Creates an Administrator at admin@egraphs.com/egraphsa
    """,

    {() =>
      demosetup.DemoScenarios.createAdmin()
    }
  )

  toScenarios add Scenario(
  "Deliver All Orders to Celebrities",
  adminCategory,
  """
  Changes All new Orders to ApprovedByAdmin, which makes them signable
  """, {
    () =>
      demosetup.DemoScenarios.approveAllOrders()
  }
  )

  private[this] def redirectToWizzle = {
    new Redirect(
      WebsiteControllers.lookupGetCelebrity("Wizzle").url
    )
  }

  //
  // Product-related members
  //
  private[this] def createAndSaveStarcraftProduct: Product = {
    Scenario.play("Will-Chan-is-a-celebrity")

    val will = Scenarios.getWillCelebrityAccount
    createProduct(celebrity = will,
      priceInCurrency=100,
      name="2010 Starcraft 2 Championships",
      description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
    )
  }

  private[this] def createProduct(celebrity: Celebrity, priceInCurrency: BigDecimal, name: String, description: String): Product = {
    val product = celebrity.newProduct.copy(priceInCurrency = priceInCurrency, name = name, description = description)
      .withPublishedStatus(PublishedStatus.Published)
      .saveWithImageAssets(image = Some(ImageIO.read(Play.getFile("test/files/longoria/product-2.jpg"))), icon = None)
    val inventoryBatch = InventoryBatch(celebrityId = celebrity.id, numInventory = 100, startDate = today, endDate = future).save()
    inventoryBatch.products.associate(product)
    product
  }

  private[this] def redirectToStarcraftProduct = {
    new Redirect(
      Utils.lookupUrl("WebsiteControllers.getCelebrityProduct", starcraftProductSlugs).url
    )
  }

  private[this] def redirectToOrderConfirmationPage(params: Map[String, String] = Map()) =
  {
    new Redirect(
      Utils.lookupUrl("WebsiteControllers.getCelebrityProduct", starcraftProductSlugs ++ params).url
    )
  }

  private[this] def starcraftProductSlugs = {
    Map(
      "celebrityUrlSlug" -> "Wizzle",
      "productUrlSlug" -> "2010-Starcraft-2-Championships"
    )
  }
}

object Scenarios {
  import AppConfig.instance

  val accountStore = instance[AccountStore]
  val celebrityStore = instance[CelebrityStore]
  val productStore = instance[ProductStore]
  val customerStore = instance[CustomerStore]

  def getWillAccount: Account = {
    accountStore.findByEmail("wchan83@egraphs.com").get
  }

  def getWillCelebrityAccount: Celebrity = {
    celebrityStore.findById(getWillAccount.celebrityId.get).get
  }

  def getWillsTwoProducts: (Product, Product) = {
    (productStore.findById(1L).get, productStore.findById(2L).get)
  }

  def getEremCustomerAccount: Customer = {
    customerStore.findById(1L).get
  }
}
