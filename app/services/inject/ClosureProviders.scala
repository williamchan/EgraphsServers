package services.inject

import com.google.inject.Provider

/**
 * Allows you to specify your providers as closures rather than
 * having to create new Provider instances. Mix this in to your ScalaModule
 * and use it like bind[MyService].toProvider {
 *   new MyServiceImpl()
 * }
 */
trait ClosureProviders {
  implicit def byNameToProvider[T](provideFn: => T): Provider[T] = {
    new Provider[T] {
      def get(): T = {
        provideFn
      }
    }
  }
}
