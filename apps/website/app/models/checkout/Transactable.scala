package models.checkout

// TODO(SER-499): Get rid of this if there's no additional benefit to it
trait Transactable[+T] { this: T =>
  /**
   * Transforms and persists a T and potentially any objects it contains, as needed.
   * @return transformed and persisted T
   */
  def transact(checkout: Checkout): T
}
