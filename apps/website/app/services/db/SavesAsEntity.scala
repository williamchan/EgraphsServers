package services.db

import org.squeryl.{Query, KeyedEntity, Table}

trait HasEntity[T <: KeyedEntity[_]] {
  def _entity: T
}

trait SavesAsEntity[ModelT <: HasEntity[EntityT], EntityT <: KeyedEntity[_]] {

  protected def modelWithNewEntity(domainModel: ModelT, entity: EntityT): ModelT

  protected def table: Table[EntityT]

  /**
   * Pimps out domain models with create and update functionality.
   */
  trait EntitySavingConversions {
    /* NOTE(SER-499): this shouldn't be defined here because the name gets clobbered when importing more than one Conversions object that extends it
    implicit def toSavingDsl(toConvert: ModelT): SavingDSL = {
     new SavingDSL(toConvert: ModelT, modelToEntity(toConvert))
    }*/

    // TODO(SER-499): may not need entity in constructor if using HasEntity[EntityT]; should it create domainModel's domain object?
    class SavingDSL(domainModel: ModelT) {

      def create(): ModelT = {
        modelWithNewEntity(domainModel, table.insert(domainModel._entity))
      }

      def update(): ModelT = {
        // TODO(SER-499) implement hooks
        table.update(domainModel._entity)

        modelWithNewEntity(domainModel, domainModel._entity)
      }
    }
  }
}