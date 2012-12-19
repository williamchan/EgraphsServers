package services

import org.squeryl.KeyedEntity

/**
 * Created with IntelliJ IDEA.
 * User: kevin
 * Date: 12/18/12
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
package object db {
  // TODO(SER-499): remove if not used
  type IsEntityOf[EntityT <: KeyedEntity[_], ModelT <: HasEntity[EntityT]] = EntityT
}
