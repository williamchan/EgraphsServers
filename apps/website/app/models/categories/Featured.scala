package models.categories

import com.google.inject.Inject

object Featured {
  val categoryName = "Featured"
  val categoryValueName = "IsFeatured"
}

/**
 * Tools to find and assign featured to entities.
 */
class Featured @Inject() (
  categoryStore: CategoryStore,
  categoryValueStore: CategoryValueStore) {

  // cost = 2 queries
  def ensureCategoryValueIsCreated(): CategoryValue = {
    val maybeFeaturedCategoryValue = categoryValueStore.findByName(Featured.categoryValueName)
    maybeFeaturedCategoryValue.getOrElse {
      val featuredCategory = ensureCategoryIsCreated()
      CategoryValue(
        categoryId = featuredCategory.id,
        name = Featured.categoryValueName,
        publicName = Featured.categoryValueName).save()
    }
  }

  // cost = 1 queries
  def ensureCategoryIsCreated(): Category = {
    categoryStore.findByName(Featured.categoryName).getOrElse {
      Category(
        name = Featured.categoryName,
        publicName = Featured.categoryName).save()
    }
  }

  // cost = 3 queries
  def updateFeaturedCelebrities(newFeaturedCelebIds: Iterable[Long]) {
    val featuredCategoryValue = ensureCategoryValueIsCreated()

    categoryValueStore.updateCelebrities(featuredCategoryValue, newFeaturedCelebIds)
  }
}