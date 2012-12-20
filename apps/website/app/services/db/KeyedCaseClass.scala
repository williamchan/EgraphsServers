package services.db

import org.squeryl.KeyedEntity

/**
 * Custom trait for case classes that extend [[services.db.Schema]]. Provides case-class
 * equality rather than the broken KeyedEntity[_] equality test that depends on the
 * mutable _isPersisted variable.
 *
 * This class was unfortunately required for reasons explained by Erem on the Squeryl newsgroup:
 * https://groups.google.com/forum/#!topic/squeryl/ouDS8maB9F0 explained by myself on the newsgroup.
 *
 * This conversation also has some good discussion of how a solution is being approached:
 * https://groups.google.com/forum/#!searchin/squeryl/case$20class/squeryl/kzhb6aJm7cA/d26KEoNLzXAJ
 */
trait KeyedCaseClass[T] extends KeyedEntity[T] {
  //
  // Abstract methods
  //
  /** Returns the result of the case class companion object's unapply method */
  def unapplied: AnyRef

  //
  // Private implementation
  //
  override def equals(other: Any): Boolean = {
    other match {
      case null =>
        false

      case keyedCaseClass: KeyedCaseClass[_] =>
        unapplied == keyedCaseClass.unapplied

      case _ =>
        super.equals(other)
    }
  }
}