package models.enums

import utils.EgraphsUnitTest
import models.enums.CallToActionType.EnumVal

trait HasCallToActionTypeTests[T <: HasCallToActionType[T]] {
  this: EgraphsUnitTest =>

  def newEntityWithCallToAction : T

  "an object with a call to action to" should "return its type" in {
    for(ctaType <- CallToActionType.values) yield {
      newEntityWithCallToAction.withCallToActionType(ctaType).callToActionType should be(ctaType)
    }
  }

  it should "throw an exception if it has an invalid type string" in {
    class HasWrongCallToActionType extends HasCallToActionType[HasWrongCallToActionType] {
      val _callToActionType = "herp"

      val callToActionTarget = "schlerp"

      def withCallToActionType(status: EnumVal) = {
        this
      }
    }

    evaluating {
      new HasWrongCallToActionType().callToActionType
    } should produce[IllegalArgumentException]
  }
}
