package services.cache

import com.google.inject.Inject

class NamespacedCache (namespace: String="", cache: Cache) extends Cache {
  def namespaced(newNamespace: String): NamespacedCache = {
    new NamespacedCache(applyNamespace(newNamespace), cache)
  }

  //
  // Cache members
  //
  def set[T](key: String, value: T, expirationSeconds: Int) {
    cache.set(applyNamespace(key), value, expirationSeconds)
  }

  def get[T: Manifest](key: String): Option[T] = {
    cache.get[T](applyNamespace(key))
  }

  def delete(key: String) {
    cache.delete(applyNamespace(key))
  }

  def clear() {
    cache.clear()
  }

  //
  // Private members
  //
  private def applyNamespace(toNamespace: String) = {
    if (namespace == "") toNamespace else namespace + "/" + toNamespace
  }
}
