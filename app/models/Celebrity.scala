package models

import play.db.jpa.QueryOn
import libs.Utils.optional
import javax.persistence.{CascadeType, OneToMany, Entity}

/**
 * Persistent entity representing the Celebrities who provide products on
 * our service.
 */
@Entity
class Celebrity extends User {
  /** Key used to secure the Celebrity's iPad */
  def apiKey = optional(_apiKey)
  def apiKey_= (newKey: String) { _apiKey = newKey }
  private var _apiKey: String = ""

  /** Textual description of the celebrity. */
  def description = optional(_description)
  def description_= (newDescription: String) { _description = newDescription }
  private var _description: String = ""

  /** The celebrity's popular name. (e.g. "Shaq" for Shaquille O'Neal) */
  def popularName = optional(_popularName)
  def popularName_= (newPopularName: String) { _popularName = newPopularName }
  private var _popularName: String = ""

  /** The celebrity's profile photograph */
  def profilePic = optional(_profilePic)
  def profilePic_= (newPic: String) { _profilePic = _profilePic }
  private var _profilePic: String = ""

  /** Products offered by the celebrity */
  def products:Iterable[Product] = {
    import scala.collection.JavaConversions._
    _products
  }

  @OneToMany(mappedBy="seller", cascade=Array(CascadeType.ALL))
  private var _products: java.util.List[Product] = new java.util.ArrayList[Product]()

  def addProduct (name: String, priceInCents: Int): Product = {
    val product = new Product(name, priceInCents, this)
    _products.add(product)
    product
  }

  def removeProduct (product: Product) {
    _products.remove(product)
  }

  override def toString = {
    "Celebrity(\"name\")"
  }
}

object Celebrity extends QueryOn[Celebrity] with UserQueryOn[Celebrity]