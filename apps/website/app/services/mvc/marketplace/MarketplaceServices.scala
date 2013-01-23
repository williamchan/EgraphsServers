package services.mvc.marketplace

import models.categories.{VerticalStore, Vertical}
import models.frontend.marketplace.{CategoryValueViewModel, CategoryViewModel, VerticalViewModel}
import com.google.inject.Inject

/**
 *  Class for marketplace services. As some of the UI elements from the marketplace become available across the website, this class will house code
 *  shared by different controllers for converting marketplace data into view models. 
 **/

case class MarketplaceServices @Inject() (verticalStore: VerticalStore) {
  /**
   * Returns viewmodels for verticals and categories form the marketplace.
    * @param maybeSelectedVertical An optional vertical to be marked as active
   * @param activeCategoryValues Any selected category values
   * @return
   */
  def getVerticalViewModels(maybeSelectedVertical: Option[Vertical]= None, activeCategoryValues: Set[Long] = Set()) : List[VerticalViewModel] = {
    verticalStore.verticals.map { v =>
      val categories = for {
        category <- v.categories
      } yield {
        CategoryViewModel(
          id = category.id,
          publicName = category.publicName,
          // TODO(sbilstein) think about making this more efficient.
          categoryValues = category.categoryValues.map( cv =>
            CategoryValueViewModel(
              publicName = cv.publicName,
              id = cv.id,
              active = activeCategoryValues.contains(cv.id)
            )
          ).toList)
      }
      VerticalViewModel(
        verticalName = v.categoryValue.name,
        publicName = v.categoryValue.publicName,
        shortName = v.shortName,
        urlSlug = v.urlSlug,
        tileUrl = v.tileUrl,
        iconUrl = v.iconUrl,
        active = v.urlSlug == maybeSelectedVertical.map(_.urlSlug).getOrElse(""),
        id = v.categoryValue.id,
        categoryViewModels = categories
      )
    }.toList
  }
}
