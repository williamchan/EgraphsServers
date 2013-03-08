package utils

import services.Time
import java.text.SimpleDateFormat
import org.joda.time.DateTime
import models._
import enums._
import egraphs.playutils.Encodings.Base64
import org.apache.commons.lang3.RandomStringUtils
import categories.{Category, CategoryValue}
import models.enums.VideoStatus
import util.Random

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
  
  val random = new Random

  def getTimeInBlobstoreFormat: String = Time.toBlobstoreFormat(Time.now)

  def generateEmail(prefix: String = "", domain: String = "egraphs.com"): String = {
    val randomInt = random.nextInt() // this is necessary since 2 calls with same parameter could produce the same time blobstore time if run in quick succession.
    prefix + getTimeInBlobstoreFormat + randomInt + "@" + domain
  }

  def generateUsername(): String = {RandomStringUtils.randomAlphabetic(30)}

  def generateFullname(): String = {
    RandomStringUtils.randomAlphabetic(10) + " " + RandomStringUtils.randomAlphabetic(10)
  }

  def generateSessionId(): String = {
    RandomStringUtils.randomAlphabetic(10)
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

  def newSavedAccount(email: Option[String] = None): Account = {
    Account(email = email.getOrElse(generateEmail(prefix = "acct-"))).withPassword(defaultPassword).right.get.save()
  }

  def newSavedAdministrator(_account: Option[Account] = None): Administrator = {
    val account = if (_account.isDefined) {
      _account.get
    } else {
      newSavedAccount()
    }

    val admin = Administrator().save()
    account.copy(administratorId = Some(admin.id)).save()
    admin
  }
  
  lazy val defaultPassword = "egraphsa"

  def newSavedCustomer(maybeAccount: Option[Account] = None): Customer = {
    val account = maybeAccount.getOrElse(newSavedAccount())
    val customer = account.createCustomer(name = "Test Customer").save()
    account.createUsername().copy(customerId = customer.id).save()
    account.copy(customerId = Some(customer.id)).save()
    customer
  }

  def newSavedCelebrity(secureInfo: Option[CelebritySecureInfo] = Some(newSavedSecureInfo())): Celebrity = {
    val fullName = generateFullname()
    val email = generateEmail(fullName.replaceAll(" ", "."))
    val acct = Account(email = email).withPassword(defaultPassword).right.get.save()
    val celeb = Celebrity(publicName = fullName, secureInfoId = secureInfo.map(_.id))
    	.withPublishedStatus(PublishedStatus.Published)
    	.withEnrollmentStatus(EnrollmentStatus.Enrolled)
    	.save()

    acct.copy(celebrityId = Some(celeb.id)).save()

    celeb
  }

  def newSavedSecureInfo(): DecryptedCelebritySecureInfo = {
    val encrypted = DecryptedCelebritySecureInfo(
      contactEmail = Some(TestData.generateEmail("bezos", "clamazon.com")),
      smsPhone = Some(RandomStringUtils.randomNumeric(10)),
      voicePhone = Some(RandomStringUtils.randomNumeric(10)),
      agentEmail = Some(TestData.generateEmail("bezosAgent", "clamazon.com")),
      streetAddress = Some(RandomStringUtils.randomAlphanumeric(40)),
      city = Some(RandomStringUtils.randomAlphanumeric(40)),
      postalCode = Some("ABC 123"),
      country = Some("CA")
    ).withDepositAccountType(Some(BankAccountType.Checking))
    .withDepositAccountRoutingNumber(Some(RandomStringUtils.randomNumeric(9).toInt))
    .withDepositAccountNumber(Some(Random.nextLong.abs)).encrypt.save()
    encrypted.decrypt
  }

  def newSavedCategory: Category = {
    Category(name = TestData.generateUsername(), publicName = TestData.generateUsername()).save()
  }

  def newSavedCategoryValue(categoryId: Long) : CategoryValue = {
    CategoryValue(name = TestData.generateUsername(), publicName = TestData.generateUsername(), categoryId = categoryId).save()
  }

  def newSavedCoupon(
    discountType: CouponDiscountType = CouponDiscountType.Flat,
    usageType: CouponUsageType = CouponUsageType.OneUse,
    maxAmount: BigDecimal = BigDecimal(25.0)
  ): Coupon = {
    Coupon(discountAmount = BigDecimal(random.nextDouble()) * maxAmount)
      .withDiscountType(CouponDiscountType.Flat)
      .withUsageType(usageType).save()
  }


  private def newProduct(celebrity: Celebrity): Product = {
    celebrity.newProduct.copy(name = "prod" + random.nextLong(), description = "some prod").withPublishedStatus(PublishedStatus.Published)
  }

  def newSavedMasthead: Masthead = {
    Masthead(name = generateUsername(), headline = generateUsername() ).save()
  }

  def newSavedProduct(celebrity: Option[Celebrity] = None): Product = {
    val product = newSavedProductWithoutInventoryBatch(celebrity)
    newSavedInventoryBatch(product)
    product
  }

  def newSavedProductWithoutInventoryBatch(celebrity: Option[Celebrity] = None): Product = {
    val product = celebrity match {
      case None => newProduct(newSavedCelebrity()).save()
      case Some(c) => newProduct(c).save()
    }
    product.saveWithImageAssets(image = Some(product.defaultPhoto.renderFromMaster), icon = None)
  }

  def newSavedProductWithoutInventoryBatch(celebrity: Celebrity): Product = {
    newSavedProductWithoutInventoryBatch(Some(celebrity))
  }

  def newSavedInventoryBatch(product: Product) : InventoryBatch = {
    val inventoryBatch = newSavedInventoryBatch(product.celebrity)
    inventoryBatch.products.associate(product)
    inventoryBatch
  }

  def newSavedInventoryBatch(celebrity: Celebrity) : InventoryBatch = {
    InventoryBatch(celebrityId = celebrity.id, numInventory = 50, startDate = TestData.today, endDate = TestData.sevenDaysHence).save()
  }
  
  def newSavedOrderStack(): (Customer, Customer, Celebrity, Product) = {
    val buyer  = TestData.newSavedCustomer()
    val recipient = TestData.newSavedCustomer()
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProduct(celebrity = Some(celebrity))
    (buyer, recipient, celebrity, product)
  }
  
  def newSavedOrder(product: Option[Product] = None): Order = {
    val customer = TestData.newSavedCustomer()
    product match {
      case Some(p) => {
        customer.buyUnsafe(p).save()
      }
      case None => {
        val p = TestData.newSavedProduct()
        customer.buyUnsafe(p).save()
      }
    }
  }

  def newFulfilledOrder(customer: Customer) : (Order, Egraph) = {
    val order = customer.buyUnsafe(TestData.newSavedProduct()).save()
    (order, order.newEgraph
      .withAssets(TestConstants.signingAreaSignatureStr, Some(TestConstants.signingAreaMessageStr), Base64.decode(TestConstants.voiceStr()))
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
      .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), Base64.decode(TestConstants.voiceStr()))
      .save()
  }

  def newSavedVideoAsset(): VideoAsset = {
    VideoAsset(_urlKey = "videoassets/fakeId/fakeVideo.mp4", _videoStatus = VideoStatus.Unprocessed.name).save()
  }
  
  // delete this if it never gets called
  def newSavedVideoAssetCelebrity(): VideoAssetCelebrity = {
    val videoAssetId = newSavedVideoAsset().id
    val celebrityId = newSavedCelebrity().id
    VideoAssetCelebrity(videoId = videoAssetId, celebrityId = celebrityId).save()
  }

  def assignToMlbCategoryValue(celebrity: Celebrity): CategoryValue = {
    val mlb = celebrity.services.categoryServices.categoryValueStore.findByName("MLB").getOrElse{
      TestData.newSavedCategoryValue(TestData.newSavedCategory.id).copy(name = "MLB").save()
    }
    celebrity.services.store.updateCategoryValues(celebrity = celebrity, categoryValueIds = List(mlb.id))
    mlb
  }

  /**
   *  Convert an iterable and key into a map with the same key for every value.
   *  Helpful when writing functional tests. 
   *  Example Usage:
   *  
   * controllers.postSomeStuff(id)(
   *    val multipleValues = List(1,2,3)
   *    FakeRequest().withFormUrlEncodedBody(
   *      toFormUrlSeq("keyForMultipleValues", multipleValues):_*
   *    ).withAuthToken
   *  )
   *
   **/

  def toFormUrlSeq[A >: Any](key: String, values: Iterable[A], stringConverter: A => String = (a: A) => a.toString) : Seq[(String, String)] = {
    values.map(value => (key -> stringConverter(value))).toSeq
  }
}
