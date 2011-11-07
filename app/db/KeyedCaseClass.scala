package db

import org.squeryl.KeyedEntity

/**
 * Custom trait for case classes that extend [[db.Schema]]. Provides case-class
 * equality rather than the broken KeyedEntity[_] equality test that depends on the
 * mutable _isPersisted variable.
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