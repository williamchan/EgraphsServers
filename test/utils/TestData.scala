package utils

import models.{Order, Celebrity, Account, Customer, Product}
import services.Time
import java.io.File
import play.Play

/**
 * Renders saved copies of domain objects that satisfy all relational integrity
 * constraints.
 */
object TestData {

  def generateEmail(prefix: String = ""): String = {
    prefix + Time.toBlobstoreFormat(Time.now) + "@egraphs.com"
  }

  def newSavedCustomer(): Customer = {
    val acct = Account(email = generateEmail(prefix = "customer-")).save()
    val cust = Customer().save()

    acct.copy(customerId = Some(cust.id)).save()

    cust
  }

  def newSavedCelebrity(): Celebrity = {
    val acct = Account(email = generateEmail(prefix = "celebrity-")).save()
    val celeb = Celebrity().save()

    acct.copy(celebrityId = Some(celeb.id)).save()

    celeb
  }

  def newSavedProduct(): Product = {
    newSavedCelebrity().newProduct.save()
  }

  def newSavedOrder(): Order = {
    val customer = newSavedCustomer()
    val product = newSavedProduct()

    customer.buy(product).save()
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