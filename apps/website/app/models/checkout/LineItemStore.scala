package models.checkout

import services.db.Schema
import services.AppConfig

/*
class LineItemStore /*@Inject()*/ (
  schema: Schema = AppConfig.instance[Schema]
) {
  apply(lineItemEntity: LineItemEntity): LineItem[_] = {
    val itemTypeEntity = from(schema.lineItemTypes) ( typeEntity =>
      where(typeEntity.id == lineItemEntity._itemTypeId)
      select(typeEntity)
    ).headOption.getOrElse(
      throw new IllegalArgumentException("Given LineItemEntity does not have a persisted LineItemType.")
    )

  }
}
*/
