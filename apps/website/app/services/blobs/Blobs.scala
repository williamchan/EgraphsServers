package services.blobs

import play.Play.configuration
import org.jclouds.blobstore.{BlobStoreContext, BlobStore}
import org.jclouds.io.Payload
import java.io._
import com.google.inject.Inject
import services.logging.Logging
import org.jclouds.blobstore.domain.Blob
import services.Time
import Time.IntsToSeconds._

/**
 * Convenience methods for storing and loading large binary data: images,
 * audio, signatures, video, etc.
 *
 * Provides Amazon-based or filesystem-based implementation based on the "blobstore" value
 * in application.conf
 *
 * Also pimps out the jclouds Blob library to include some useful data transformations to/from
 * Blobs, Payloads, Array[Byte]s, Strings, Files, and Streams.
 *
 * To use, import Blobs.Conversions._ into whatever scope.
 */
class Blobs @Inject() (blobProvider: BlobVendor) extends Logging {
  import Blobs._

  /**
   * Tests whether a blob with the provided key exists
   *
   * @param key the key to test
   * @return true that a blob with the given key exists
   */
  def exists(key: String): Boolean = {
    blobProvider.exists(blobstoreNamespace, key)
  }

  /**
   * Retrieve the Blob at a given key.
   */
  def get(key: String): Option[Blob] = {
    blobProvider.get(blobstoreNamespace, key)
  }

  /**
   * Retrieve the Blob at a given key from the static resources blobstore. Always attempts to access Amazon S3.
   * Delete this method after Play 2.0. Use getStaticResourceUrl instead.
   */
  @Deprecated
  def getStaticResource(key: String) : Option[Blob] = {
    val store: BlobStore = S3BlobVendor.context.getBlobStore
    Option(store.getBlob(staticResourceBlobstoreNamespace, key))
  }

  /**
   * Returns a short-lived signed URL to the blob location denoted by the key parameter in the static resource S3 bucket.
   * This URL follows the query string authentication specification as described in this documentation:
   * http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html. Note that a URL will be returned even if
   * the key does not point to any resource, ie passing in "IDoNotExist" as the key will still return a signed URL.
   */
  def getStaticResourceUrl(key: String, expirationSeconds: Int = 5.minutes): String = {
    val expires = System.currentTimeMillis() / 1000 + expirationSeconds
    val baseUrl = S3BlobVendor.context.getSigner.signGetBlob(staticResourceBlobstoreNamespace, key).getEndpoint
    val signature = S3BlobVendor.sign(namespace = staticResourceBlobstoreNamespace, key = key, expires = expires)
    baseUrl + "?" + "AWSAccessKeyId=" + S3BlobVendor.s3id + "&Expires=" + expires + "&Signature=" + signature
  }

  /**
   * UNSAFE. Retrieves the URL for the provided key. Favor getUrlOption
   */
  def getUrl(key: String): String = {
    getUrlOption(key).get
  }

  /**
   * Returns the public URL for the Blob. The URL will only work if
   * the blob stored is publicly available.
   */
  def getUrlOption(key: String): Option[String] = {
    blobProvider.urlOption(blobstoreNamespace, key)
  }

  /**
   * Puts data into a Blob object.
   *
   * @param key the key by which to store the data
   * @param bytes the data to store
   * @param access the Blobstore access policy for this object. This will only be enforced
   *   against Amazon S3.
   */
  def put(key: String, bytes: Array[Byte], access: AccessPolicy=AccessPolicy.Private) {
    blobProvider.put(blobstoreNamespace, key, bytes, access)
  }

  /** Delete the data at a certain key. */
  def delete(key: String) {
    // wchan: We don't need a delete method... Hopefully, we don't ever have to delete assets. But if we do, this method
    // should use blobProvider and not blobStore directly.
    blobStore.removeBlob(blobstoreNamespace, key)
  }

  /**
   * Clears the entire Blobstore. Only runs if "blobstore.allowscrub" is
   * set to "yes" in application.conf.
   */
  def scrub() {
    val applicationMode = configuration.get("application.mode")
    log("Checking application.mode before scrubbing blobstore. Must be in dev mode. Mode is: " + applicationMode)
    if (applicationMode != "dev") {
      throw new IllegalStateException("Cannot scrub blobstore unless in dev mode")
    }

    configuration.get("blobstore.allowscrub") match {
      case "yes" =>
        blobStore.clearContainer(blobstoreNamespace)

      case _ =>
        throw new IllegalStateException(
          """I'm not going to scrub the blobstore unless "blobstore.allowscrub"
          is set to "yes" in application.conf"""
        )
    }
  }

  /**
   * Retrieve the actual Blobstore when more intricate use of the
   * jclouds api is needed.
   */
  def blobStore: BlobStore = {
    blobProvider.context.getBlobStore
  }

  /**
   * Verifies configuration information and bootstraps the egraphs container
   * in the blobstore.
   */
  def init() = {
    checkAvailable()

    if (!blobStore.containerExists(blobstoreNamespace)) {
      blobStore.createContainerInLocation(null, blobstoreNamespace)
    }
  }

  /**
   * Retrieve the jclouds BlobStore context when more intricate use of the
   * jclouds api is needed.
   */
  def context: BlobStoreContext = {
    blobProvider.context
  }

  /**
   * Verifies that the blobstore is correctly configured, as far as it can
   * without actually trying to open the store.
   */
  def checkAvailable() {
    require(
      blobstoreType != null,
      """
      application.conf: A "blobstore" configuration value of either "s3" or
      "filesystem" is required
      """
    )

    require(
      blobstoreNamespace != null,
      """
      application.conf: A "blobstore.namespace" value is required. On s3 this
      is the bucket name.
      """
    )

    blobProvider.checkConfiguration()
  }
}


/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */

object Blobs {
  /** Key for the blobstore in application config */
  private[blobs] val blobstoreConfigKey = "blobstore"

  /** Type of blobstore. See "blobstore" in application.conf */
  private[blobs] val blobstoreType = configuration.getProperty(blobstoreConfigKey)

  /** Namespace of blobstore; equivalent to S3's bucket */
  private[blobs] val blobstoreNamespace = configuration.getProperty("blobstore.namespace")

  /** Namespace of blobstore that holds static resources; equivalent to S3's bucket */
  private[blobs] val staticResourceBlobstoreNamespace = configuration.getProperty("staticresources.blobstore.namespace")

  /**
   * Gives Blobs access to RichPayload. Also allows more variance in inputs to put()
   * (e.g. Strings and Files)
   */
  object Conversions {
    implicit def blobToRichPayload(blob: Blob): RichPayload = {
      payloadToRichPayload(blob.getPayload)
    }

    implicit def payloadToRichPayload(payload: Payload): RichPayload = {
      new RichPayload(payload)
    }

    implicit def stringToByteArray(string: String): Array[Byte] = {
      string.getBytes
    }

    implicit def fileToByteArray(file: File): Array[Byte] = {
      val bytes = new Array[Byte](file.length.toInt)
      new BufferedInputStream(new FileInputStream(file)).read(bytes)

      bytes
    }
  }

  /**
   * Pimping of the jclouds Blob library to make content access easier.
   */
  class RichPayload(payload: Payload) {

    def asInputStream: BufferedInputStream = {
      new BufferedInputStream(payload.getInput)
    }

    def asIntStream: Stream[Int] = {
      val stream = asInputStream

      Stream
        .continually(stream.read)
        .takeWhile(theInt => theInt != -1)
    }

    def asByteStream: Stream[Byte] = {
      asIntStream.map(theInt => theInt.toByte)
    }

    def asByteArray: Array[Byte] = {
      val bytesOut = new ByteArrayOutputStream()

      val is = asInputStream
      var nextByte = is.read()

      while (nextByte != -1) {
        bytesOut.write(nextByte)
        nextByte = is.read()
      }

      is.close()
      bytesOut.toByteArray
    }

    def asString: String = {
      new String(asByteArray, "UTF-8")
    }
  }
}

