import java.io.File
import libs.Blobs
import models.{Customer, Product, Account, Celebrity}
import play.mvc.results.{Redirect, Result, ScalaAction}
import play.mvc.Router
import scenario.{Scenario, DeclaresScenarios}
import Blobs.Conversions._

/**
 * All scenarios supported by the API.
 */
class Scenarios extends DeclaresScenarios {
  toScenarios add Scenario(
    "Will-Chan-is-a-celebrity",

    """
    Creates a celebrity named William 'Wizzle' Chan. His login/password are
    wchan83@gmail.com/herp. He has a profile photo.
    """,

   {() =>
      val celebrity = Celebrity(
        firstName=Some("William"),
        lastName=Some("Chan"),
        popularName=Some("Wizzle")
      ).save()

      Account(email="wchan83@gmail.com",
              celebrityId=Some(celebrity.id)
      ).withPassword("herp").right.get.save()

     celebrity.saveWithProfilePhoto(new File("./test/files/will_chan_celebrity_profile.jpg"))
   }
  )


  toScenarios add Scenario(
    "Will-has-two-products",

    """
    Adds two products to Wizzle's product portfolio. The first costs $100 and
    is an image of him at the 2010 Starcraft 2 Championships. The second costs
    $200 and is an image of him at the 2011 King of Pweens Competition, where he
    first established dominance in the arena of Pweendom.
    """,

    {() =>
      val will = getWillCelebrityAccount

      will.newProduct.copy(
        priceInCents=10000,
        description="Starcraft 2 Championships, 2010"
      ).save()

      will.newProduct.copy(
        priceInCents=7000,
        description="King of Pweens Competition, 2011"
      ).save()
    }
  )

  toScenarios add Scenario(
    "Erem-is-a-customer",

    """
    Creates a customer named Erem Boto.

    [note: He does not have any associated Account thus far...but we can create one
     programmatically later on if we want to log in as him]
    """,

    {() =>
      Customer(name="Erem Boto").save()
    }
  )

  toScenarios add Scenario(
    "Erem-buys-Wills-two-products-twice-each",

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
    "A-public-image-is-on-the-blobstore",

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
    "Celebrity-page-with-two-products",

    """ Opens up Wizzle's celebrity page with two products """,

    {() =>
      Scenario.clearAll()
      
      Scenario.play(
        "Will-Chan-is-a-celebrity",
        "Will-has-two-products"
      )

      redirect("CelebrityController.index", Map("celebrityName" -> "Wizzle"))
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

  def redirect(controller: String, params:Map[String, Object]=Map()) = {
    import scala.collection.JavaConversions._
    
    val actionDef = Router.reverse(controller, params)

    new Redirect(actionDef.url)
  }
  
  def openPage(controller: => Any): Result =
  {
    new ScalaAction(controller)
  }
}

