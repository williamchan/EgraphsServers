package utils

import com.google.inject.util.Modules
import services.AppConfig
import com.google.inject.{Module, Guice}

/**
 * Version of AppConfig that overrides some application bindings with other modules
 * @param moduleOverride module containing the bindings that should override
 *     the default application bindings
 */
class TestAppConfig(moduleOverride: Module) {
  import uk.me.lings.scalaguice.InjectorExtensions._

  /**
   * Get an instance of an injectable class from the injector. See [[services.AppConfig.instance]]
   */
  def instance[T: Manifest] = {
    fakeInjector.instance[T]
  }

  private val fakeInjector = {
    val customModule = Modules.`override`(new AppConfig()).`with`(moduleOverride)
    Guice.createInjector(customModule)
  }
}
