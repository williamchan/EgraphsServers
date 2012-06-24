package services.cache

/**
 * Trait for any class that implements a Cache in our system. Caches are simple key value stores.
 *
 * Usage:
 *
 * {{{
 *   class MyClass @Inject() (cacheFactory: CacheFactory) {
 *     // Import a helper class that will make duration-specification easier
 *     import services.Time.IntsToSeconds._
 *
 *     // Setting the object is simple. Make SURE that the object implements Serializable
 *     // or it will fail at runtime.
 *     def cache = cacheFactory.applicationCache
 *     cache.set("a string", "herp", 1.second)
 *     cache.set("a number", 1234, 1.second)
 *     cache.set("a list", List(1,2,3,4), 1.second)
 *     cache.set("a map", Map("herp" -> "derp"), 1.minute + 10.seconds)
 *
 *     // Get an object by type-parameterizing the .get method. Be careful,
 *     // because if you get the type parameter wrong then it'll blow up
 *     // at run-time when you try to use the instance.
 *     println("string is: " + cache.get[String]("a string"))
 *     println("number is: " + cache.get[Number]("a number"))
 *     println("list is: " + cache.get[List[Int]]("a list"))
 *     println("map is: " + cache.get[Map[String, String]]("a map"))
 *
 *     // Delete the object
 *     cache.delete("a map")
 *   }
 * }}}
 */
trait Cache {
  /**
   * Sets a Serializable object into the cache on the specified key for a particular amount of time.
   * Overwrites any existing values.
   *
   * The values you pass in must be Serializable or it will fail at run-time.
   *
   * @param key the key to insert.
   * @param value the value to insert. It can be any serializable type.
   * @param expirationSeconds the number of seconds before the key gets automatically
   *     flushed from the cache.
   * @tparam T type of the value.
   */
  def set[T](key: String, value: T, expirationSeconds: Int)

  /**
   * Retrieves a serializable object identified by the `key` from the cache, or
   * None if it wasn't found.
   *
   * Due to type-erasure, the type checking here is not and can not be strong.
   * So just make sure you never query out
   *
   * @param key the key to look up
   * @tparam T the type to get
   * @return Some(whatYouWereLookingFor) if found, otherwise None
   */
  def get[T : Manifest](key: String): Option[T]

  /**
   * Deletes the record mapped to the provided key, if a record was there.
   *
   * @param key key of the record to delete.
   */
  def delete(key: String)

  /**
   * Clears the entire cache. Avoid doing this except on an ad-hoc basis.
   */
  def clear()
}
