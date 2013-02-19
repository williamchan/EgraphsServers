package services.db

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._

/**
 * Provides findById and get to Models with KeyedEntities; use QueriesAsModel for models that
 * are themselves KeyedEntities.
 */
trait QueriesAsEntity[ModelT <: HasEntity[EntityT, KeyT], EntityT <: KeyedEntity[KeyT], KeyT] {
  protected def entityToModel(convert: EntityT): Option[ModelT]
  protected def table: Table[EntityT]

  def findEntityById(id: KeyT) = table.lookup(id)

  def findById(id: KeyT): Option[ModelT] = {
    for (entity <- table.lookup(id); model <- entityToModel(entity)) yield model
  }

  def get(id: KeyT)(implicit m: Manifest[EntityT]): ModelT = {
    findById(id).getOrElse(
      throw new RuntimeException(
        "DB contained no instances of class " + m.erasure.getName + " with id=" + id
      )
    )
  }
}
