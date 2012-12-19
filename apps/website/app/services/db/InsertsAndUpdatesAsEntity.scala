package services.db

import org.squeryl.{KeyedEntity, Table}



trait HasEntity[T <: KeyedEntity[_]] { def _entity: T }


trait InsertsAndUpdatesAsEntity[ModelT <: HasEntity[EntityT], EntityT <: KeyedEntity[_]]
  extends InsertsAndUpdates[EntityT]
{
  // NOTE(SER-499): this can be removed if HasEntity has "def withEntity: HasEntity[T]"
  protected def modelWithNewEntity(domainModel: ModelT, entity: EntityT): ModelT

  protected def table: Table[EntityT]

  def insert(model: ModelT): ModelT = {
    modelWithNewEntity(
      model,
      this.insert(model._entity)
    )
  }

  def update(model: ModelT): ModelT = {
    modelWithNewEntity(
      model,
      this.update(model._entity)
    )
  }
}
