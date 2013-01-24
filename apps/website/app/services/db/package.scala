package services

import org.squeryl.KeyedEntity

package object db {
  // TODO(SER-499): remove if not used
  type IsEntityOf[EntityT <: KeyedEntity[_], ModelT <: HasEntity[EntityT, _]] = EntityT
}
