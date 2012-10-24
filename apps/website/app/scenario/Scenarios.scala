package scenario

import services.db.Schema
import services.blobs.Blobs
import egraphs.playutils.Encodings.Base64
import org.apache.commons.mail.HtmlEmail
import org.squeryl.PrimitiveTypeMode._
import models._
import play.api.mvc.Results.Redirect
import controllers.WebsiteControllers
import utils.TestConstants
import enums._
import services.{Utils, AppConfig}
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import javax.imageio.ImageIO
import controllers.website.GetAccountSettingsEndpoint
import services.http.EgraphsSession._
import scala.Some
import models.Administrator
import models.InventoryBatch
import models.Order
import play.api.Play
import play.api.Play.current
import utils.{TestData, TestConstants}

/**
 * All scenarios supported by the API.
 */
class Scenarios extends DeclaresScenarios {
  // Categories of scenario
  private val adminCategory = "Admin Helpers"
  private val apiCategory = "API Helpers"
  private val servicesCategory = "Services Helpers"

  private val accountSettingsPage = "Account Settings Page"
  private val egraphPageCategory = "Egraph Page"
  private val galleryPageCategory = "My Gallery Page"
  private val productPageCategory = "Product Page"

  private val schema = AppConfig.instance[Schema]
  private val mail = AppConfig.instance[services.mail.TransactionalMail]

  private lazy val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  private lazy val today = DateTime.now().toLocalDate.toDate
  private lazy val future = dateFormat.parse("2020-01-01")
  //TODO sbilstein this page really needs some refactoring

  //
  // adminCategory
  //
  toScenarios add Scenario(
  "Create Admin",
  adminCategory,
  """
    Creates an Administrator at admin@egraphs.com/egraphsa
  """, {
    () =>
      Scenarios.createAdmin()
  }
  )

  toScenarios add Scenario(
  "Enroll All Celebrities",
  adminCategory,
  """
  Changes All Celebrities to be Enrolled status
  """, {
    () =>
      import org.squeryl.Query
      import org.squeryl.PrimitiveTypeMode._
      val celebrities: Query[(Celebrity)] = from(schema.celebrities)(
        (c) => select(c)
      )
      for (celebrity <- celebrities) {
        celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()
      }
  }
  )

  toScenarios add Scenario(
  "Deliver All Orders to Celebrities",
  adminCategory,
  """
  Changes All new Orders to ApprovedByAdmin, which makes them signable
  """, {
    () =>
      Scenarios.approveAllOrders()
  }
  )

  //
  // apiCategory
  //

  toScenarios add Scenario(
  "Will Chan is a celebrity",
  apiCategory,
  """
    Creates a celebrity named William 'Wizzle' Chan. His login/password are
    wchan83@egraphs.com/derp. He has a profile photo.
  """, {
    () =>
      Scenarios.createAdmin()
      import Blobs.Conversions._
      val celebrity = Celebrity(
        publicName = "Wizzle",
        bio = "Love my fans from New York to Tokyo, from Seoul to the Sudetenland. And for all you haters out there -- don't mess around. I sleep with one eye closed, the other fixed on my Vespene gas supply.",
        organization = "Major League Baseball",
        isFeatured = true
      ).withPublishedStatus(PublishedStatus.Published).save()
      Account(email = "wchan83@egraphs.com",
        celebrityId = Some(celebrity.id)
      ).withPassword(TestData.defaultPassword).right.get.save()
      celebrity.saveWithProfilePhoto(Play.getFile("test/resources/will_chan_celebrity_profile.jpg"))
      celebrity.withLandingPageImage(Play.getFile("test/resources/ortiz_masthead.jpg")).save()
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
  """, {
    () =>
      val will = Scenarios.getWillCelebrityAccount
      val photoImage = Some(Product().defaultPhoto.renderFromMaster)
      val iconImage = Some(Product().defaultIcon.renderFromMaster)
      val product1 = will.addProduct(
        name = "2010 Starcraft 2 Championships",
        description = "Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one.",
        priceInCurrency = 10,
        image = photoImage,
        icon = iconImage,
        storyTitle = "The story and the glory",
        storyText = "{signer_link}{signer_name}{end_link} was born on top. He proved it to the world at the {product_link}{product_name}{end_link}. A few days afterwards he got a note from {recipient_name} on his iPad. This was his response.",
        publishedStatus = PublishedStatus.Published
      )
      val product2 = will.addProduct(
        name = "2011 King of Pweens Competition",
        description = "In classic form, Wizzle dominated the competition and left mouths agape.",
        priceInCurrency = 70,
        storyTitle = "The story and the glory",
        image = Some(Product().defaultPhoto.renderFromMaster),
        icon = iconImage,
        storyText = """
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
  """, {
    () =>
      val customer = TestData.newSavedCustomer().copy(name = "Erem Boto").save()
      customer.account.withPassword(TestData.defaultPassword).right.get.save()
  }
  )

  toScenarios add Scenario(
  "Erem buys Wills two products twice each",
  apiCategory,
  """Creates two unfulfilled orders, one each ordered against Will's two products.""", {
    () =>
      val erem = Scenarios.getEremCustomerAccount
      val (starcraftChampionship, kingOfPweensCompetition) = Scenarios.getWillsTwoProducts
      erem.buy(starcraftChampionship, recipientName = "Erem Boto", requestedMessage = Some("Happy 13th birthday, Don!"), messageToCelebrity = Some("My buddy Don is your biggest fan!")).save()
      erem.buy(kingOfPweensCompetition, recipientName = "Erem Boto", requestedMessage = Some("Happy Pweenday, Don!"), messageToCelebrity = Some("Don loves everything you do!")).save()
  }
  )

  toScenarios add Scenario(
  "Will fulfills one of Erem's product orders",
  apiCategory,
  """Will fulfills one of Erem's product orders""", {
    () =>
      val (starcraftChampionship, kingOfPweensCompetition) = Scenarios.getWillsTwoProducts
      val firstOrder = from(schema.orders)(order =>
        where(order.id in List(starcraftChampionship.id, kingOfPweensCompetition.id))
          select (order)
      ).headOption.get
      firstOrder
        .withPaymentStatus(PaymentStatus.Charged).save()
        .newEgraph
        .withAssets(TestConstants.signingAreaSignatureStr, Some(TestConstants.signingAreaMessageStr), Base64.decode(TestConstants.voiceStr()))
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
  """Will fulfills Erem's second product order""", {
    () =>
      val (starcraftChampionship, _) = Scenarios.getWillsTwoProducts
      val secondOrder = from(schema.orders)(order =>
        where(order.id in List(starcraftChampionship.id))
          select (order)
      ).headOption.get
      secondOrder
        .newEgraph
        .withAssets(TestConstants.signingAreaSignatureStr, Some(TestConstants.signingAreaMessageStr), Base64.decode(TestConstants.voiceStr()))
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
  """, {
    () =>
      Scenarios.getWillCelebrityAccount.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()
  }
  )

  toScenarios add Scenario(
  "Will failed faux-enrollment in biometric services",
  apiCategory,
  """
    Marks celebrity Will as having failed enrollment in biometrics. Does not _actually_
    attempt to make biometric profiles for him on real services.
  """, {
    () =>
      Scenarios.getWillCelebrityAccount.withEnrollmentStatus(EnrollmentStatus.FailedEnrollment).save()
  }
  )

  //
  // servicesCategory
  //
  toScenarios add Scenario(
  "Send account confirmation email to will@egraphs.com",
  servicesCategory,
  """Sends account confirmation email to will@egraphs.com using the configured SMTP server""", {
    () =>
      val email = new HtmlEmail()
      email.setFrom("noreply@egraphs.com")
      email.addReplyTo("noreply@egraphs.com")
      email.addTo("will@egraphs.com")
      email.setSubject("Welcome to Egraphs!")
      val verifyPasswordUrl = "https://www.google.com"
      val html = views.html.frontend.email_account_verification(verifyPasswordUrl = verifyPasswordUrl)
      email.setHtmlMsg(html.toString())
      val textVersion = views.html.frontend.email_account_verification_text(verifyPasswordUrl)
      email.setTextMsg(textVersion.toString())
      mail.send(email)
  }
  )

  toScenarios add Scenario(
  "Send order confirmation email to will@egraphs.com",
  servicesCategory,
  """Sends order confirmation email to will@egraphs.com using the configured SMTP server""", {
    () =>
      val email = new HtmlEmail()
      email.setFrom("noreply@egraphs.com", "Egraphs")
      email.addTo("will@egraphs.com")
      email.setSubject("Order Confirmation")
      val html = views.html.frontend.email_order_confirmation(
        buyerName = "Will Chan",
        recipientName = "Andrew Smith",
        recipientEmail = "me@egraphs.com",
        celebrityName = "Celebrity Joe",
        productName = "Product 1",
        orderDate = "Jan 1, 2012",
        orderId = "1234",
        pricePaid = "$50.00",
        deliveredByDate = "Jan 8, 2012",
        faqHowLongLink = "/faq#how-long",
        hasPrintOrder = true
      )
      email.setHtmlMsg(html.toString())
      val textVersion = views.html.frontend.email_order_confirmation_text(
        buyerName = "Will Chan",
        recipientName = "Andrew Smith",
        recipientEmail = "me@egraphs.com",
        celebrityName = "Celebrity Joe",
        productName = "Product 1",
        orderDate = "Jan 1, 2012",
        orderId = "1234",
        pricePaid = "$50.00",
        deliveredByDate = "Jan 8, 2012",
        faqHowLongLink = "/faq#how-long",
        hasPrintOrder = true
      )
      email.setTextMsg(textVersion.toString())
      mail.send(email)
  }
  )

  toScenarios add Scenario(
  "Send egraph fulfilled email to will@egraphs.com",
  servicesCategory,
  """Sends egraph fulfilled email to will@egraphs.com using the configured SMTP server""", {
    () =>
      val email = new HtmlEmail()
      email.setFrom("celebrity@egraphs.com", "Celebrity Jane")
      email.addTo("will@egraphs.com")
      email.addReplyTo("noreply@egraphs.com")
      email.setSubject("I just finished signing your Egraph")
      val viewEgraphUrl = "http://www.google.com"
      val html = views.html.frontend.email_view_egraph(
        viewEgraphUrl = viewEgraphUrl,
        celebrityName = "Celebrity Jane",
        recipientName = "Will Chan"
      )
      email.setHtmlMsg(html.toString())
      val textVersion = views.html.frontend.email_view_egraph_text(viewEgraphUrl = viewEgraphUrl, celebrityName = "Celebrity Jane", recipientName = "Will Chan")
      email.setTextMsg(textVersion.toString())
      mail.send(email)
  }
  )

  //
  // accountSettingsPage
  //
  toScenarios add Scenario(
  "Bring up the account settings page",
  accountSettingsPage,
  """Changes All new Orders to ApprovedByAdmin, which makes them signable""", {
    () =>
      Scenario.play("Erem-is-a-customer")      
      Redirect(controllers.routes.WebsiteControllers.getAccountSettings).withSession(Key.CustomerId.name -> "1")
  }
  )

  //
  // egraphPageCategory
  //
  toScenarios add Scenario(
  "Valid, ordered landscape Egraph",
  egraphPageCategory,
  """Creates a signed Egraph and views its page""", {
    () =>
      Scenario.clearAll()
      Scenario.play(
        "Will-Chan-is-a-celebrity",
        "Will-has-two-products",
        "Erem-is-a-customer",
        "Erem-buys-Wills-two-products-twice-each",
        "Deliver-All-Orders-to-Celebrities",
        "Will-fulfills-one-of-Erems-product-orders"
      )
      Redirect(controllers.routes.WebsiteControllers.getEgraph(2).url)
  }
  )

  //
  // galleryPageCategory
  //
  toScenarios add Scenario(
  "Gallery with 1 fulfilled egraph",
  galleryPageCategory,
  """Creates one signed egraph and displays the gallery not logged in""", {
    () =>
      Scenario.clearAll()
      Scenario.play(
        "Will-Chan-is-a-celebrity",
        "Will-has-two-products",
        "Erem-is-a-customer",
        "Erem-buys-Wills-two-products-twice-each",
        "Deliver-All-Orders-to-Celebrities",
        "Will-fulfills-one-of-Erems-product-orders"
      )
      Redirect("/account/1/gallery")
  }
  )

  toScenarios add Scenario(
  "Gallery with 2 fulfilled egraphs",
  galleryPageCategory,
  """Creates two signed egraphs and displays the gallery not logged in""", {
    () =>
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
      Redirect("/account/1/gallery")
  }
  )

  //
  // productPageCategory
  //
  toScenarios add Scenario(
  "Accessed from Celebrity Page or from URL",
  productPageCategory,
  """Displays the product page as if it had been accessed via the link on the Celebrity page.""", {
    () =>
      Scenario.clearAll()
      createAndSaveStarcraftProduct
      redirectToStarcraftProduct
  }
  )

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
      .saveWithImageAssets(image = Some(ImageIO.read(Play.getFile("test/resources/longoria/product-2.jpg"))), icon = None)
    val inventoryBatch = InventoryBatch(celebrityId = celebrity.id, numInventory = 100, startDate = today, endDate = future).save()
    inventoryBatch.products.associate(product)
    product
  }

  private[this] def redirectToStarcraftProduct = {
    Redirect(controllers.routes.WebsiteControllers.getStorefrontChoosePhotoCarousel("Wizzle", "2010-Starcraft-2-Championships"))
  }
}

object Scenarios {
  import AppConfig.instance
  private val schema = instance[Schema]
  private val accountStore = instance[AccountStore]
  private val celebrityStore = instance[CelebrityStore]
  private val productStore = instance[ProductStore]
  private val customerStore = instance[CustomerStore]

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

  def createAdmin() {
    val adminEmail = "admin@egraphs.com"
    if (accountStore.findByEmail(adminEmail).isEmpty) {
      val administrator = Administrator().save()
      Account(email = adminEmail, administratorId = Some(administrator.id)).withPassword("egraphsa").right.get.save()
    }
  }

  def approveAllOrders() {
    import org.squeryl.Query
    import org.squeryl.PrimitiveTypeMode._
    val orders: Query[(Order)] = from(schema.orders)(
      (o) => where(o._reviewStatus === OrderReviewStatus.PendingAdminReview.name) select (o)
    )
    for (order <- orders) {
      order.withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
    }
  }
}
