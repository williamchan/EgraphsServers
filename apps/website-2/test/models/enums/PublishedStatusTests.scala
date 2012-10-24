package models.enums


import utils.EgraphsUnitTest
import PublishedStatus._

/**
 * Adds tests for model classes that have implement HasPublishedStatus
 * @tparam T
 */
trait HasPublishedStatusTests[T <: HasPublishedStatus[T]] {
  this: EgraphsUnitTest =>

  def newPublishableEntity: T

  "a publishable object" should "return its status" in {
    newPublishableEntity.withPublishedStatus(Published).publishedStatus should be(Published)
    newPublishableEntity.withPublishedStatus(Unpublished).publishedStatus should be(Unpublished)
  }

  "a publishable object" should "throw an exception if it has an invalid status string" in {

    class HasWrongPublishableString extends HasPublishedStatus[HasWrongPublishableString] {
      val _publishedStatus = "herp"

      def withPublishedStatus(status: EnumVal) = {
        this
      }
    }

    evaluating {
      new HasWrongPublishableString().publishedStatus
    } should produce[IllegalArgumentException]
  }

}


