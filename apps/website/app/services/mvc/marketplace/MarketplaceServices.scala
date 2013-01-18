package services.mvc.marketplace

import models.categories.{VerticalStore, Vertical}
import models.frontend.marketplace.{CategoryValueViewModel, CategoryViewModel, VerticalViewModel}
import com.google.inject.Inject

case class MarketplaceServices @Inject() (verticalStore: VerticalStore) {
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
