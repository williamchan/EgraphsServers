package models.categories

import com.google.inject.Inject

object Internal {
  val categoryName = "Internal"
}

/**
 * Helper to setup the Internal category. This category is meant to
 * house miscellaneous internal categories values (e.g. [[models.categories.Featured]])
 */
class Internal @Inject() (categoryStore: CategoryStore) {

  // cost = 1 queries
  def ensureCategoryIsCreated(): Category = {
    categoryStore.findByName(Internal.categoryName).getOrElse {
      Category(
        name = Internal.categoryName,
        publicName = Internal.categoryName).save()
    }
  }
}