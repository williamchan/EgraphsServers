package services.config

/** 
 * Readable alternatives for the accessors built into [[play.api.Configuration]]
 **/
private[config] trait ConfigPropertyAccessors {
  protected def playConfig: play.api.Configuration

  protected final def string(key: String, options: String*): String = {
    val maybeOptions = if (options.size > 0) Some(options.toSet) else None
    playConfig.getString(key, maybeOptions).getOrElse(
      throw new IllegalArgumentException("application config string for key \"" + key + "\" is required")
    )
  }

  protected final def boolean(key: String): Boolean = {
    playConfig.getBoolean(key).getOrElse(
      throw new IllegalArgumentException("application config boolean for key \"" + key + "\" is required")
    )
  }

  protected final def int(key: String): Int = {
    playConfig.getInt(key).getOrElse(
      throw new IllegalArgumentException("application config integer for key \"" + key + "\" is required")
    )
  }
}