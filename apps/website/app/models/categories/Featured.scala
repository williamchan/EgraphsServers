package models.categories

import com.google.inject.Inject
import models.Celebrity
import models.CelebrityStore

object Featured {
  val categoryValueName = "Featured"
}

/**
 * Tools to find and assign the "internal -> featured" category value
 * to entities.
 */
class Featured @Inject() (
  internal: Internal,
  categoryValueStore: CategoryValueStore,
  celebrityStore: CelebrityStore) {

  lazy val categoryValue: CategoryValue = {
    val maybeFeaturedCategoryValue = categoryValueStore.findByName(Featured.categoryValueName)
    maybeFeaturedCategoryValue.getOrElse {
      val featuredCategory = internal.category
      CategoryValue(
        categoryId = featuredCategory.id,
        name = Featured.categoryValueName,
        publicName = Featured.categoryValueName).save()
    }
  }

  def updateFeaturedCelebrities(newFeaturedCelebIds: Iterable[Long]) {
    val featuredCategoryValue = categoryValue

    // cost = 3 queries
    categoryValueStore.updateCelebrities(featuredCategoryValue, newFeaturedCelebIds)
  }
}