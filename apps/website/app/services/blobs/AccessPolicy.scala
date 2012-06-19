package services.blobs


/**
 * Constants that determine how Blobs can be accessed.
 */
sealed trait AccessPolicy

object AccessPolicy {
  /** Accessible via a public URL */
  case object Public extends AccessPolicy

  /** Inaccessible via public URL */
  case object Private extends AccessPolicy
}

