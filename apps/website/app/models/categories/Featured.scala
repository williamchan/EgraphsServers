package models.categories

import com.google.inject.Inject

object Featured {
  val categoryValueName = "Featured"
}

/**
 * Tools to find and assign featured to entities.
 */
class Featured @Inject() (
  internal: Internal,
  categoryValueStore: CategoryValueStore) {

  // cost = 2 queries
  def ensureCategoryValueIsCreated(): CategoryValue = {
    val maybeFeaturedCategoryValue = categoryValueStore.findByName(Featured.categoryValueName)
    maybeFeaturedCategoryValue.getOrElse {
      val featuredCategory = internal.ensureCategoryIsCreated()
      CategoryValue(
        categoryId = featuredCategory.id,
        name = Featured.categoryValueName,
        publicName = Featured.categoryValueName).save()
    }
  }

  // cost = 5 queries
  def updateFeaturedCelebrities(newFeaturedCelebIds: Iterable[Long]) {
    val featuredCategoryValue = ensureCategoryValueIsCreated()

    // cost = 3 queries
    categoryValueStore.updateCelebrities(featuredCategoryValue, newFeaturedCelebIds)
  }
}