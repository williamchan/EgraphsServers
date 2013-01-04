package services.db

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._

trait QueriesAsEntity[ModelT, EntityT <: KeyedEntity[KeyT], KeyT] {
  protected def entityToModel(convert: EntityT): Option[ModelT]
  protected def table: Table[EntityT]

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
