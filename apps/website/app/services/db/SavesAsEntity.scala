package services.db

import org.squeryl.{Query, KeyedEntity, Table}

trait SavesAsEntity[ModelT, EntityT <: KeyedEntity[_]] {
   protected def modelToEntity(domainModel: ModelT): EntityT
   protected def modelWithNewEntity(domainModel: ModelT, entity: EntityT): ModelT

   protected def table: Table[EntityT]

  /**
   * Pimps out domain models with create and update functionality.
   */
   trait EntitySavingConversions {
     implicit def toSavingDsl(toConvert: ModelT): SavingDSL = {
       new SavingDSL(toConvert: ModelT, modelToEntity(toConvert))
     }

     class SavingDSL(domainModel: ModelT, entity: EntityT) {
       def create(): ModelT = {
         modelWithNewEntity(domainModel, table.insert(entity))
       }

       def update(): ModelT = {
         // TODO(SER-499) implement hooks
         table.update(entity)

         modelWithNewEntity(domainModel, entity)
       }
     }
   }
 }

