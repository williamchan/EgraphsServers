package services.db

import org.squeryl.{KeyedEntity, Table}



trait HasEntity[T <: KeyedEntity[KeyT], KeyT] {
  /**
   * id is not defined as _entity.id so one may, for instance, take id as a constructor argument to a class
   * and retrieve the _entity lazily.
   */
  def id: KeyT
  def _entity: T
}


trait InsertsAndUpdatesAsEntity[ModelT <: HasEntity[EntityT, _], EntityT <: KeyedEntity[_]]
  extends InsertsAndUpdates[EntityT]
{
  // NOTE(SER-499): could be moved into HasEntity as "def withEntity: HasEntity[T, KeyT]" if cleaner
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
