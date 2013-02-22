package models.checkout.forms

import enums.ApiError
import play.api.data.{Mapping, FormError, Forms}
import play.api.data.format.{Formats, Formatter}
import models.{CouponStore, ProductStore}
import services.AppConfig.instance

/**
 * Use similar to play.api.data.Forms -- provides Mappings and Constraints for checkout forms that conform to the
 * Checkout API's error spec.
 */
object ApiForms {
  import play.api.data.validation._

  //
  // Mappings
  //
  def text(min: Int = 0, max: Int = Int.MaxValue): Mapping[String] = text verifying lengthRequirement(min, max)
  def text: Mapping[String] = Forms.of[String](apiStringFormat)
  def email = text verifying formatRequirement(playEmailPattern)
  def longNumber = Forms.of[Long](apiLongFormat)
  def boolean = Forms.of[Boolean](apiBooleanFormat)

  /** like text(min = 1) except gives ApiError.Required instead of ApiError.InvalidLength if violated */
  def nonEmpty(mapping: Mapping[String]) = mapping verifying isNonEmpty


  //
  // Constraints -- API length and format errors
  //
  def lengthRequirement(min: Int = 0, max: Int = Int.MaxValue) = Constraint[String] { (input: String) =>
    if (min to max contains input.size) Valid
    else Invalid(ApiError.InvalidLength.name)
  }

  def formatRequirement(pattern: String) = Constraint[String] { (input: String) =>
    if (input matches pattern) Valid
    else Invalid(ApiError.InvalidFormat.name)
  }

  def validProductId(implicit productStore: ProductStore = instance[ProductStore]) = Constraint[Long] { (id: Long) =>
    val product = productStore.findById(id).headOption
    val availableInventory = product flatMap (_.availableInventoryBatches.headOption)
    (product, availableInventory) match {
      case (Some(_), Some(_)) => Valid
      case (Some(_), None) => Invalid(ApiError.NoInventory.name)
      case (None, _) => Invalid(ApiError.InvalidProduct.name)
    }
  }

  def validCouponCode(implicit couponStore: CouponStore = instance[CouponStore]) = Constraint[String] { (code: String) =>
    couponStore.findValid(code) match {
      case Some(_) => Valid
      case None => Invalid(ApiError.InvalidCouponCode.name)
    }
  }

  /** Like a more vague `lengthRequirement(min = 1)`, returns ApiError.Required if violated */
  def isNonEmpty = Constraint[String] { (input: String) =>
    if (input.isEmpty) Invalid(ApiError.Required.name)
    else Valid
  }


  //
  // Formatters -- for API type error
  //
  private val apiLongFormat = apiFormatter[Long]( input => input.toLong )
  private val apiBooleanFormat = apiFormatter[Boolean]( input => input.toBoolean )
  private val apiStringFormat = new Formatter[String] {
    // modified from play.api.data.format.Formats.stringFormat
    def unbind(key: String, value: String) = Map(key -> value)
    def bind(key: String, data: Map[String, String]) = data.get(key).toRight(Seq(FormError(key, ApiError.Required.name)))
  }

  private def apiFormatter[T](convert: String => T): Formatter[T] = new Formatter[T] {
    def unbind(key: String, value: T) = Map(key -> value.toString)

    def bind(key: String, data: Map[String, String]) = {
      def bindString = apiStringFormat.bind(key, data)
      def invalidTypeError = Left { Seq( FormError(key, ApiError.InvalidType.name) ) }
      def errorOrConverted(input: String) = try Right(convert(input)) catch { case _: Throwable => invalidTypeError }

      bindString.right flatMap { (input: String) => errorOrConverted(input) }
    }
  }


  //
  // Helpers
  //
  private val playEmailPattern = """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b"""
}
