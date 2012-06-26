package services

/**
 * Represents classes that can be separated into functionally equivalent sub-units,
 * e.g. for making a different keyspace in a cache, or folders in a file system.
 */
trait Namespacing {
  /** Title of the namespace, e.g. "shopping-cart" */
  def namespace: String

  /**
   * applies the namespace to the string. For example, if the namespace
   * were "shopping-cart", then `applyNamespace("order1")` == shopping-cart/order1
   */
  protected def applyNamespace(to: String): String = {
    if (namespace == "") to else namespace + "/" + to
  }
}
