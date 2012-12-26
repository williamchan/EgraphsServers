package models.checkout

trait Transactable[+T] { this: T =>
  /**
   * Transforms and persists a T and potentially any objects it contains, as needed.
   * @return transformed and persisted T
   */
  def transact(): T
}
