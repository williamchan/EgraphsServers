package services

trait Namespacing {
  def namespace: String

  protected def applyNamespace(to: String): String = {
    if (namespace == "") to else namespace + "/" + to
  }
}
