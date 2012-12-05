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

  // this has a side effect of creating the category if it isn't there
  lazy val category: Category = {
    categoryStore.findByName(Internal.categoryName).getOrElse {
      Category(
        name = Internal.categoryName,
        publicName = Internal.categoryName).save()
    }
  }
}