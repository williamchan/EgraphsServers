import controllers.api.CelebrityOrderApiControllers.EgraphFulfillmentHandler
import controllers.CelebrityProductController.EgraphPurchaseHandler
import db.Schema
import java.io.File
import libs.{Utils, Blobs}
import models.{Customer, Product, Account, Celebrity}
import org.apache.commons.mail.SimpleEmail
import play.libs.Mail
import play.mvc.results.Redirect
import play.Play
import scenario.{Scenario, DeclaresScenarios}
import Blobs.Conversions._
import utils.{TestConstants, TestData}
import org.squeryl.PrimitiveTypeMode._
import TestData.Longoria

/**
 * All scenarios supported by the API.
 */
class Scenarios extends DeclaresScenarios {
  val apiCategory = "API Helpers"
  val celebrityPageCategory = "Celebrity Page"
  val productPageCategory = "Product Page"
  val orderConfirmationPageCategory = "Order Confirmation Page"
  val egraphPageCategory = "Egraph Page"

  toScenarios add Scenario(
    "Send an email to erem@egraphs.com",

    apiCategory,

    """Sends an email to erem@egraphs.com using the configured SMTP server""",

    {() =>
      val email = new SimpleEmail();

      email.setMailSession(Mail.getSession)
      email.setFrom("ehboto@gmail.com");
      email.setSubject("Hi");
      email.setMsg("This is a test mail ... :-)");
      email.addTo("erem@egraphs.com", "Erem Boto");

      libs.Mail.send(email)
    }

  )

  toScenarios add Scenario(
    "Will Chan is a celebrity",

    apiCategory,

    """
    Creates a celebrity named William 'Wizzle' Chan. His login/password are
    wchan83@gmail.com/herp. He has a profile photo.
    """,

   {() =>
      val celebrity = Celebrity(
        firstName=Some("William"),
        lastName=Some("Chan"),
        publicName=Some("Wizzle"),
        description=Some("Love my fans from New York to Tokyo, from Seoul to the Sudetenland. And for all you haters out there -- don't mess around. I sleep with one eye closed, the other fixed on my Vespene gas supply.")
      ).save()

      Account(email="wchan83@gmail.com",
              celebrityId=Some(celebrity.id)
      ).withPassword("herp").right.get.save()

     celebrity.saveWithProfilePhoto(new File("./test/files/will_chan_celebrity_profile.jpg"))
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
      val will = getWillCelebrityAccount

      will.newProduct.copy(
        priceInCurrency=10,
        name="2010 Starcraft 2 Championships",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      ).save()

      will.newProduct.copy(
        priceInCurrency=70,
        name="2011 King of Pweens Competition",
        description="In classic form, Wizzle dominated the competition and left mouths agape."
      ).save()
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
    Creates four unfulfilled orders, each ordered twice against Will's
    two products.
    """,

    {() =>
      val erem = getEremCustomerAccount
      val (starcraftChampionship, kingOfPweensCompetition) = getWillsTwoProducts

      erem.buy(starcraftChampionship).save()
      erem.buy(kingOfPweensCompetition).save()
    }
  )

  toScenarios add Scenario(
    "Will fulfills one of Erem's product orders",

    apiCategory,

    """
    Creates four unfulfilled orders, each ordered twice against Will's
    two products.
    """,

    {() =>
      val will = getWillCelebrityAccount
      val (starcraftChampionship, kingOfPweensCompetition) = getWillsTwoProducts

      val firstOrder = from(Schema.orders)(order =>
        where(order.id in List(starcraftChampionship.id, kingOfPweensCompetition.id))
        select (order)
      ).headOption.get

      EgraphFulfillmentHandler(
        TestConstants.signatureStr,
        TestConstants.voiceStrPercentEncoded,
        firstOrder,
        will
      ).execute()
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
      Blobs.put("a/b/derp.jpg", new File("./test/files/derp.jpg"))
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

      val will = getWillCelebrityAccount
      will.newProduct.copy(
        priceInCurrency=100,
        name="2010 Starcraft 2 Championships",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      ).save()

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

      val will = getWillCelebrityAccount

      will.newProduct.copy(
        priceInCurrency=100,
        name="2010 Starcraft 2 Championships",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      ).save()

      will.newProduct.copy(
        priceInCurrency=90,
        name="2012 Platinum League Victory",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      ).save()

      will.newProduct.copy(
        priceInCurrency=89.99,
        name="2001 Senior Yearbook Photo",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      ).save()

      will.newProduct.copy(
        priceInCurrency=100.50,
        name="Bi-Annual World Series of Magic: The Gathering",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      ).save()

      will.newProduct.copy(
        priceInCurrency=120.00,
        name="2200AD Undead League Starcraft II Championship",
        description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
      ).save()

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
      val celebrity = getWillCelebrityAccount

      EgraphPurchaseHandler(
        recipientName = "Erem Boto",
        recipientEmail = "ehboto@gmail.com",
        buyerName = "Rooster McGillycuddy",
        buyerEmail = "rooster@egraphs.com",
        stripeTokenId = TestData.newStripeToken().getId,
        desiredText = Some("Happy 29th birthday, Erem!"),
        personalNote = Some("I'm your biggest fan!"),
        celebrity=celebrity,
        product=product
      ).execute()
    }
  )

  toScenarios add Scenario(
      "Valid, ordered eGraph",

      egraphPageCategory,

      """
      Creates a signed eGraph and views its page
      """,

      {() =>
        Scenario.clearAll()

        Scenario.play(
          "Will-Chan-is-a-celebrity",
          "Will-has-two-products",
          "Erem-is-a-customer",
          "Erem-buys-Wills-two-products-twice-each",
          "Will-fulfills-one-of-Erems-product-orders"
        )

        new Redirect(
          Utils.lookupUrl("EgraphController.egraph", Map("orderId" -> "1")).url
        )
      }
    )


  def getWillAccount: Account = {
    Account.findByEmail("wchan83@gmail.com").get
  }

  def getWillCelebrityAccount: Celebrity = {
    Celebrity.findById(getWillAccount.celebrityId.get).get
  }

  def getWillsTwoProducts: (Product, Product) = {
    (Product.findById(1L).get, Product.findById(2L).get)
  }

  def getEremCustomerAccount: Customer = {
    Customer.findById(1L).get
  }

  def redirectToWizzle = {
    new Redirect(
      Utils.lookupUrl("CelebrityController.index", Map("celebrityUrlSlug" -> "Wizzle")).url
    )
  }

  //
  // Product-related members
  //
  def createAndSaveStarcraftProduct: Product = {
    Scenario.play("Will-Chan-is-a-celebrity")

    val will = getWillCelebrityAccount
    will.newProduct.copy(
      priceInCurrency=100,
      name="2010 Starcraft 2 Championships",
      description="Before this classic performance nobody had dreamed they would ever see a resonance cascade, let alone create one."
    ).save()
  }

  def redirectToStarcraftProduct = {
    new Redirect(
      Utils.lookupUrl("CelebrityProductController.index", starcraftProductSlugs).url
    )
  }

  def redirectToOrderConfirmationPage(params: Map[String, String] = Map()) =
  {
    new Redirect(
      Utils.lookupUrl("CelebrityProductController.buy", starcraftProductSlugs ++ params).url
    )
  }

  def starcraftProductSlugs = {
    Map(
      "celebrityUrlSlug" -> "Wizzle",
      "productUrlSlug" -> "2010-Starcraft-2-Championships"
    )
  }
}

