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

  /**
   * Gets all the celebrities that are published and featured.
   *
   * Note: This query could be optimized into 1 query when there is a performance need.
   */
  def featuredPublishedCelebrities: Iterable[Celebrity] = {
    // this is usually 1 query
    val featuredCelebritiesMaybePublished = categoryValue.celebrities

    // this is 1 query
    val publishedCelebrities = celebrityStore.getPublishedCelebrities.groupBy(celebrity => celebrity.id)

    for {
      featuredCelebrity <- featuredCelebritiesMaybePublished
      if publishedCelebrities.contains(featuredCelebrity.id)
    } yield {
      publishedCelebrities(featuredCelebrity.id).head // we know this can only be one element
    }
  }
}