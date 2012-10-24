package services.inject

/** 
 * Trait encompasses both the Google and the JSR330 implementations of Provider.
 * 
 * Extend this trait instead of either of the others because otherwise we get runtime exceptions
 * from Guice for god-knows-what reason.
 **/
trait InjectionProvider[T] extends com.google.inject.Provider[T] with javax.inject.Provider[T]
