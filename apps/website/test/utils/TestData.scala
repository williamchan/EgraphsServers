package utils

import services.{AppConfig, Time}
import util.Random
import java.text.SimpleDateFormat
import org.joda.time.DateTime
import models._
import enums.{EgraphState, PublishedStatus}
import play.libs.Codec

/**
 * Renders saved copies of domain objects that satisfy all relational integrity
 * constraints.
 */
object TestData {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  lazy val jan_01_2012 = dateFormat.parse("2012-01-01")
  lazy val jan_08_2012 = dateFormat.parse("2012-01-08")
  lazy val feb_01_2012 = dateFormat.parse("2012-02-01")
  lazy val today = DateTime.now().toLocalDate.toDate
  lazy val tomorrow = new DateTime().plusDays(1).toLocalDate.toDate
  lazy val twoDaysHence = new DateTime().plusDays(2).toLocalDate.toDate
  lazy val threeDaysHence = new DateTime().plusDays(3).toLocalDate.toDate
  lazy val sevenDaysHence = new DateTime().plusDays(7).toLocalDate.toDate

  lazy val defaultPassword = "egraphsa"

  val random = new Random

  def getTimeInBlobstoreFormat: String = Time.toBlobstoreFormat(Time.now)

  def generateEmail(prefix: String = "", domain: String = "egraphs.com"): String = {
    prefix + getTimeInBlobstoreFormat + "@" + domain
  }

  def makeTestCacheKey: String = {
    "this_is_a_test_case_key_" + new java.util.Date().getTime
  }

  def newSavedAddress(account: Option[Account] = None): Address = {
    Address(accountId = account.getOrElse(newSavedAccount()).id,
      addressLine1 = "615 2nd Ave",
      addressLine2 = Option("Suite 300"),
      city = "Seattle",
      _state = "WA",
      postalCode = "98104"
    ).save()
  }

  def newSavedAccount(): Account = {
    Account(email = generateEmail(prefix = "acct-")).save()
  }

  def newSavedAdministrator(_account: Option[Account] = None): Administrator = {
    val account = if (_account.isDefined) {
      _account.get
    } else {
      newSavedAccount()
    }

    val admin = Administrator().save()
    account.copy(administratorId = Some(admin.id))
    admin
  }

  def newSavedCustomer(): Customer = {
    val acct = Account(email = generateEmail(prefix = "customer-")).withPassword(defaultPassword).right.get.save()
    val cust = acct.createCustomer(name = "Test Customer").save()
    acct.copy(customerId = Some(cust.id)).save()
    cust
  }

  def newSavedCelebrity(): Celebrity = {
    val identifier = getTimeInBlobstoreFormat
    val email = "celebrity-" + identifier + "@egraphs.com"
    val acct = Account(email = email).save()
    val celeb = Celebrity(publicName = "Celebrity " + identifier).withPublishedStatus(PublishedStatus.Published).save()

    acct.copy(celebrityId = Some(celeb.id)).save()

    celeb
  }

  private def newProduct(celebrity: Celebrity): Product = {
    celebrity.newProduct.copy(name = "prod" + random.nextLong(), description = "some prod").withPublishedStatus(PublishedStatus.Published)
  }

  def newSavedProduct(celebrity: Option[Celebrity] = None): Product = {
    var product = celebrity match {
      case None => newProduct(newSavedCelebrity()).save()
      case Some(c) => newProduct(c).save()
    }
    product = product.saveWithImageAssets(image = Some(product.defaultPhoto.renderFromMaster), icon = None)
    val inventoryBatch = newSavedInventoryBatch(product.celebrity)
    inventoryBatch.products.associate(product)
    product
  }

  def newSavedProductWithoutInventoryBatch(celebrity: Celebrity): Product = {
    newProduct(celebrity).save()
  }

  def newSavedInventoryBatch(celebrity: Celebrity) : InventoryBatch = {
    InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.today, endDate = TestData.sevenDaysHence).save()
  }

  def newSavedOrder(product: Option[Product] = None): Order = {
    val customer = TestData.newSavedCustomer()
    product match {
      case Some(p) => {
        customer.buy(p).save()
      }
      case None => {
        val p = TestData.newSavedProduct()
        customer.buy(p).save()
      }
    }
  }

  def newFulfilledOrder(customer: Customer) : (Order, Egraph) = {
    val order = customer.buy(TestData.newSavedProduct()).save()
    (order, order.newEgraph
      .withAssets(TestConstants.signingAreaSignatureStr, Some(TestConstants.signingAreaMessageStr), Codec.decodeBASE64(TestConstants.voiceStr()))
      .save()
      .withYesMaamBiometricServices
      .verifyBiometrics
      .withEgraphState(EgraphState.Published)
      .save())

  }

  def newSavedEgraph(orderOption: Option[Order] = None): Egraph = {
    val order = orderOption match {
      case None => newSavedOrder()
      case Some(o) => o
    }
    order.newEgraph
      .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), TestConstants.fakeAudio)
      .save()
  }

  def newSavedEgraphWithRealAudio(): Egraph = {
    newSavedOrder().newEgraph
      .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), Codec.decodeBASE64(TestConstants.voiceStr()))
      .save()
  }

  def newControllers: TestWebsiteControllers = {
    AppConfig.instance[TestWebsiteControllers]
  }
}