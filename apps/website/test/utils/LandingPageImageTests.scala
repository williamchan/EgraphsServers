package utils

import models.LandingPageImage
import org.apache.commons.io.IOUtils
import play.api.Play._

trait LandingPageImageTests[T <: LandingPageImage[T]] {
  this: EgraphsUnitTest =>

  def newEntityWithLandingPageImage: T

  private lazy val defaultImage  = IOUtils.toByteArray(current.resourceAsStream("images/1550x556.jpg").get)

  "an object with a Landing Page Image" should "have a blob key" in {
    val (entity, image) = newEntityWithLandingPageImage.withLandingPageImage(defaultImage)
    entity._landingPageImageKey should not be(None)
  }

}
