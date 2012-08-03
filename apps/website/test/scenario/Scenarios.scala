package scenario

import services.db.Schema
import services.blobs.Blobs
import org.apache.commons.mail.HtmlEmail
import play.mvc.results.Redirect
import utils.{TestConstants, TestData}
import org.squeryl.PrimitiveTypeMode._
import models._
import controllers.WebsiteControllers
import controllers.website.EgraphPurchaseHandler
import enums.{EgraphState, EnrollmentStatus, PublishedStatus}
import play.libs.Codec
import services.payment.Payment
import play.Play
import services.{Utils, AppConfig}
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import javax.imageio.ImageIO
import controllers.website.GetAccountSettingsEndpoint
import services.http.EgraphsSession
import controllers.website.consumer.CelebrityLandingConsumerEndpoint

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
  private val accountSettingsPage = "Account Settings Page"
  private val productPageCategory = "Product Page"
  private val orderConfirmationPageCategory = "Order Confirmation Page"
  private val egraphPageCategory = "Egraph Page"
  private val galleryPageCategory = "My Gallery"

  private val mail = AppConfig.instance[services.mail.Mail]

  private lazy val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  private lazy val today = DateTime.now().toLocalDate.toDate
  private lazy val future = dateFormat.parse("2020-01-01")
  //TODO sbilstein this page really needs some refactoring

  toScenarios add Scenario(
    "Send account confirmation email to will@egraphs.com",
    apiCategory,
    """Sends account confirmation email to will@egraphs.com using the configured SMTP server""",
    {() =>
      val email = new HtmlEmail()
      email.setFrom("noreply@egraphs.com")
      email.addReplyTo("noreply@egraphs.com")
      email.addTo("will@egraphs.com")
      email.setSubject("Welcome to Egraphs!")
      val emailLogoSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-logo.jpg")))
      val emailFacebookSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-facebook.jpg")))
      val emailTwitterSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-twitter.jpg")))
      val verifyPasswordUrl = "https://www.google.com";
      val html = views.frontend.html.email_account_verification(
        verifyPasswordUrl = verifyPasswordUrl,
        emailLogoSrc = emailLogoSrc,
        emailFacebookSrc = emailFacebookSrc,
        emailTwitterSrc = emailTwitterSrc
      )
      email.setHtmlMsg(html.toString())
      val textVersion = views.frontend.html.email_account_verification_text(verifyPasswordUrl)
      email.setTextMsg(textVersion.toString())
      mail.send(email)
    }
  )

  toScenarios add Scenario(
  "Send order confirmation email to will@egraphs.com",
  apiCategory,
  """Sends order confirmation email to will@egraphs.com using the configured SMTP server""",
  {() =>
    val email = new HtmlEmail()
    email.setFrom("noreply@egraphs.com", "Egraphs")
    email.addTo("will@egraphs.com")
    email.setSubject("Order Confirmation")
    val emailLogoSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-logo.jpg")))
    val emailFacebookSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-facebook.jpg")))
    val emailTwitterSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-twitter.jpg")))
    val html = views.frontend.html.email_order_confirmation(
      buyerName = "Will Chan",
      recipientName = "Andrew Smith",
      recipientEmail = "me@egraphs.com",
      celebrityName = "Celebrity Joe",
      productName = "Product 1",
      orderDate = "Jan 1, 2012",
      orderId = "1234",
      pricePaid = "$50.00",
      deliveredyDate = "Jan 8, 2012",
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
    )
    email.setHtmlMsg(html.toString())
    val textVersion = views.frontend.html.email_order_confirmation_text(
      buyerName = "Will Chan",
      recipientName = "Andrew Smith",
      recipientEmail = "me@egraphs.com",
      celebrityName = "Celebrity Joe",
      productName = "Product 1",
      orderDate = "Jan 1, 2012",
      orderId = "1234",
      pricePaid = "$50.00",
      deliveredyDate = "Jan 8, 2012"
    )
    email.setTextMsg(textVersion.toString())
    mail.send(email)
  }
  )

  toScenarios add Scenario(
  "Send egraph fulfilled email to will@egraphs.com",
  apiCategory,
  """Sends egraph fulfilled email to will@egraphs.com using the configured SMTP server""",
  {() =>
    val email = new HtmlEmail()
    email.setFrom("celebrity@egraphs.com", "Celebrity Jane")
    email.addTo("will@egraphs.com")
    email.addReplyTo("noreply@egraphs.com")
    email.setSubject("I just finished signing your Egraph")
    val emailLogoSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-logo.jpg")))
    val emailFacebookSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-facebook.jpg")))
    val emailTwitterSrc = "cid:"+email.embed(Play.getFile(Utils.asset("public/images/email-twitter.jpg")))
    val viewEgraphUrl = "http://www.google.com"
    val html = views.frontend.html.email_view_egraph(
      viewEgraphUrl = viewEgraphUrl,
      celebrityName = "Celebrity Jane",
      recipientName = "Will Chan",
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
    )
    email.setHtmlMsg(html.toString())
    val textVersion = views.frontend.html.email_view_egraph_text(viewEgraphUrl = viewEgraphUrl, celebrityName = "Celebrity Jane", recipientName = "Will Chan")
    email.setTextMsg(textVersion.toString())
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
     import Blobs.Conversions._

      val celebrity = Celebrity(
        publicName="Wizzle",
        bio="Love my fans from New York to Tokyo, from Seoul to the Sudetenland. And for all you haters out there -- don't mess around. I sleep with one eye closed, the other fixed on my Vespene gas supply.",
        organization = "Major League Baseball",
        isFeatured = true
      ).withPublishedStatus(PublishedStatus.Published).save()

      Account(email="wchan83@egraphs.com",
              celebrityId=Some(celebrity.id)
      ).withPassword(TestData.defaultPassword).right.get.save()

     celebrity.saveWithProfilePhoto(Play.getFile("./test/files/will_chan_celebrity_profile.jpg"))
     celebrity.withLandingPageImage(Play.getFile("./test/files/ortiz_masthead.jpg")).save()
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
        image=Some(Product().defaultPhotoPortrait.renderFromMaster),
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
    "Will fulfills Erem's second product order",
    apiCategory,
    """
      Will fulfills Erem's second product order
    """,

    {() =>
      val (starcraftChampionship, kingOfPweensCompetition) = Scenarios.getWillsTwoProducts

      val secondOrder = from(schema.orders)(order =>
        where(order.id in List(starcraftChampionship.id))
          select (order)
      ).headOption.get

      secondOrder
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
    should be accessible via /blob/files/a/b/derp.jpg
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
  "Alpha Users",
  celebrityPageCategory,
  """Create Lots of Users""",

  {() =>
    Scenario.clearAll()
    createCelebrity("Erem Boto", "erem@egraphs.com")
    createCelebrity("Andrew Smith", "andrew@egraphs.com")
    createCelebrity("David Auld", "david@egraphs.com")
    createCelebrity("Eric Feeny", "eric@egraphs.com")
    createCelebrity("Will Chan", "will@egraphs.com")
    createCelebrity("Zach Apter", "zachapter@gmail.com")
    createCelebrity("Brian Auld", "bauld@raysbaseball.com")
    createCelebrity("Michael Kalt", "mkalt@raysbaseball.com")
    createCelebrity("Matt Silverman", "msilverman@raysbaseball.com")
    createCelebrity("Gabe Kapler", "gabe@egraphs.com")
    createCelebrity("Mike Ginal", "mike@egraphs.com")
    createCelebrity("J Cohn", "j@egraphs.com")
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
        product = product,
        totalAmountPaid = product.price,
        billingPostalCode = "55555"
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
    "1 valid, fulfilled signed egraphs",
    galleryPageCategory,

    """
      Creates one signed egraphs and displays the gallery not logged in
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

      new Redirect("/account/1/gallery")
    }
  )

  toScenarios add Scenario(
    "2 valid, fulfilled signed egraphs",
    galleryPageCategory,

    """
        Creates one signed egraphs and displays the gallery not logged in
    """,
    {() =>
      Scenario.clearAll()
      Scenario.play(
        "Will-Chan-is-a-celebrity",
        "Will-has-two-products",
        "Erem-is-a-customer",
        "Erem-buys-Wills-two-products-twice-each",
        "Deliver-All-Orders-to-Celebrities",
        "Will-fulfills-one-of-Erems-product-orders",
        "Will-fulfills-Erems-second-product-order"
      )
      new Redirect("/account/1/gallery")
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

  toScenarios add Scenario(
    "Bring up the account settings page",
    accountSettingsPage,
    """
    Changes All new Orders to ApprovedByAdmin, which makes them signable
    """, {
    () =>
      Scenario.play(
        "Erem-is-a-customer"
      )
      egraphsSession.withLong(EgraphsSession.Key.CustomerId, 1).save()
      new Redirect(GetAccountSettingsEndpoint.url().url)
    }
  )


  private[this] def redirectToWizzle = {
    new Redirect(
      CelebrityLandingConsumerEndpoint.url("Wizzle").url
    )
  }

  //
  // Product-related members
  //
  private def egraphsSession:EgraphsSession = {
    AppConfig.instance[() => EgraphsSession].apply()
  }

  private[this] def createAndSaveStarcraftProduct: Product = {
    Scenario.play("Will-Chan-is-a-celebrity")

    val will = Scenarios.getWillCelebrityAccount
    createProduct(celebrity = will,
      priceInCurrency=100,
      name="2010 Starcraft 2 Championships",
      description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
    )
  }

  private def createCelebrity(name: String, email: String) {
    import javax.imageio.ImageIO
    import play.Play
    val celebrity = Celebrity(publicName = name, bio = "Today's Sriracha is tomorrow's salsa.", isFeatured = true)
      .withEnrollmentStatus(EnrollmentStatus.Enrolled).withPublishedStatus(PublishedStatus.Published).save()
    Account(email = email, celebrityId = Some(celebrity.id)).withPassword("egraphsa").right.get.save()
    val inventoryBatch = InventoryBatch(celebrityId = celebrity.id, numInventory = 100, startDate = today, endDate = future).save()
    for (i <- 1 until 8) yield {
      val product = celebrity.newProduct.copy(priceInCurrency = 100, name = celebrity.publicName + "'s Product " + i, description = "Help me... help YOU...")
        .withPublishedStatus(PublishedStatus.Published)
        .saveWithImageAssets(image = Some(ImageIO.read(Play.getFile("test/files/kapler/product-1.jpg"))), icon = None)
      inventoryBatch.products.associate(product)
    }
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
      Utils.lookupUrl("WebsiteControllers.getStorefrontChoosePhotoCarousel", starcraftProductSlugs).url
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
    celebrityStore.get(getWillAccount.celebrityId.get)
  }

  def getWillsTwoProducts: (Product, Product) = {
    (productStore.get(1L), productStore.get(2L))
  }

  def getEremCustomerAccount: Customer = {
    customerStore.get(1L)
  }
}

