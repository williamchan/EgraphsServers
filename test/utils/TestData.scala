package utils

import services.Time
import java.io.File
import play.Play
import util.Random
import java.text.SimpleDateFormat
import org.joda.time.DateTime
import models._

/**
 * Renders saved copies of domain objects that satisfy all relational integrity
 * constraints.
 */
object TestData {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  lazy val jan_01_2012 = dateFormat.parse("2012-01-01")
  lazy val feb_01_2012 = dateFormat.parse("2012-02-01")
  lazy val today = DateTime.now().toLocalDate.toDate
  lazy val tomorrow = new DateTime().plusDays(1).toLocalDate.toDate
  lazy val twoDaysHence = new DateTime().plusDays(2).toLocalDate.toDate
  lazy val threeDaysHence = new DateTime().plusDays(3).toLocalDate.toDate
  lazy val sevenDaysHence = new DateTime().plusDays(7).toLocalDate.toDate

  val random = new Random

  def generateEmail(prefix: String = ""): String = {
    prefix + Time.toBlobstoreFormat(Time.now) + "@egraphs.com"
  }

  def newSavedCustomer(): Customer = {
    val acct = Account(email = generateEmail(prefix = "customer-")).save()
    val cust = Customer(name = "testcustomer").save()

    acct.copy(customerId = Some(cust.id)).save()

    cust
  }

  def newSavedCelebrity(): Celebrity = {
    val email = generateEmail(prefix = "celebrity-")
    val acct = Account(email = email).save()
    val celeb = Celebrity(publicName = Some(email)).save()

    acct.copy(celebrityId = Some(celeb.id)).save()

    celeb
  }

  def newSavedProduct(celebrity: Option[Celebrity] = None): Product = {
    var product = celebrity match {
      case None => newSavedCelebrity().newProduct.save()
      case Some(c) => c.newProduct.copy(name = "prod" + random.nextLong()).save()
    }
    product = product.saveWithImageAssets(image = Some(product.defaultPhoto.renderFromMaster), icon = None)
    val inventoryBatch = newSavedInventoryBatch(product.celebrity)
    inventoryBatch.products.associate(product)
    product
  }

  def newSavedProductWithoutInventoryBatch(celebrity: Celebrity): Product = {
    celebrity.newProduct.copy(name = "prod" + random.nextLong()).save()
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


  object Longoria {
    require(fileBase.exists(), "Evan Longoria test photos were not found at " + fileBase.getAbsoluteFile)

    val profilePhoto = longoFile("profile.jpg")
    val productPhotos = Array(longoFile("product-1.jpg"), longoFile("product-2.jpg"), longoFile("product-3.jpg"))

    private val fileBase = Play.getFile("test/files/longoria")

    private def longoFile(filename: String): File = {
      Play.getFile(fileBase + "/" + filename)
    }
  }

  object Kapler {
    val productPhotos = Array(Play.getFile("test/files/kapler/product-1.jpg"))
  }

}