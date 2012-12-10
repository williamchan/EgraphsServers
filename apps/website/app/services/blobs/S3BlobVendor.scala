package services.blobs

import org.jclouds.blobstore.BlobStoreContextFactory
import org.jclouds.aws.s3.AWSS3Client
import org.jclouds.s3.domain.CannedAccessPolicy
import services.logging.Logging
import services.config.ConfigFileProxy
import services.http.HttpContentService
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import egraphs.playutils.Encodings.{ Base64, URL }
import services.config.ConfigFileProxy
import com.google.inject.Inject
import services.Time.IntsToSeconds._
import org.joda.time.DateTimeConstants

/** [[services.blobs.Blobs.BlobProvider]] implementation backed by Amazon S3 */
private[blobs] case class S3BlobVendor @Inject() (
  config: ConfigFileProxy,
  httpContent: HttpContentService) extends BlobVendor with Logging {
  val s3id = config.s3Id
  val s3secret = config.s3Secret
  val cacheControlValue = "max-age" + config.immutableAssetsCacheControlInSeconds

  //
  // BlobVendor members
  //
  override def context = {
    new BlobStoreContextFactory().createContext("aws-s3", s3id, s3secret)
  }

  override def urlOption(namespace: String, key: String): Option[String] = {
    context.getBlobStore.blobMetadata(namespace, key) match {
      case null => None
      case metadata if metadata.getUri == null => None
      case metadata => Some(metadata.getUri.toURL.toString)
    }
  }

  override def secureUrlOption(namespace: String, key: String, expirationSeconds: Int = 5 minutes): Option[String] = {
    val expires = System.currentTimeMillis() / DateTimeConstants.MILLIS_PER_SECOND  + expirationSeconds
    val baseUrl = this.context.getSigner.signGetBlob(namespace, key).getEndpoint
    val signature = this.sign(namespace = namespace, key = key, expires = expires)

    Some(baseUrl + "?" + "AWSAccessKeyId=" + this.s3id + "&Expires=" + expires + "&Signature=" + signature)
  }

  override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    import org.jclouds.s3.options.PutObjectOptions.Builder.withAcl

    val s3: AWSS3Client = context.getProviderSpecificContext.getApi

    val s3Object = s3.newS3Object()

    // Set the data
    s3Object.setPayload(data)

    // Set blob metadata
    val metadata = s3Object.getMetadata
    metadata.setKey(key)

    // Set content type and encoding (where applicable)
    val contentInfo = httpContent.headersForFilename(key)
    val contentMetadata = metadata.getContentMetadata
    contentMetadata.setContentType(contentInfo.contentType)
    for (encoding <- contentInfo.contentEncoding) contentMetadata.setContentEncoding(encoding)

    // Since we will not be changing content in these blobs we will want any clients (also the CDN)
    // to cache these for a long time
    metadata.setCacheControl(cacheControlValue)

    // Ship it off
    s3.putObject(namespace, s3Object, withAcl(s3ConstantForAccessPolicy(access)))
  }

  override def checkConfiguration() {
    require(
      s3id != null,
      """
      application.conf: An "s3.id" configuration must be provided.
      """)
    require(
      s3secret != null,
      """
      application.conf: An "s3.secret" configuration must be provided.
      """)
  }

  /**
   * Creates a signature for securely accessing an S3 location per the query string parameter specification as described
   * in http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html.
   *
   * @param namespace S3 bucket name
   * @param key S3 resource key
   * @param expires expiration time in epoch time
   * @param awsSecretKey AWS Secret Access Key
   * @return signature to be used in authenticated REST request
   */
  private[blobs] def sign(namespace: String, key: String, expires: Long, awsSecretKey: String = s3secret): String = {
    val msg = "GET\n\n\n" + expires + "\n/" + namespace + "/" + key
    val mac = Mac.getInstance("HmacSHA1")
    val keyBytes = awsSecretKey.getBytes("UTF8")
    val signingKey = new SecretKeySpec(keyBytes, "HmacSHA1")
    mac.init(signingKey)
    val digest = mac.doFinal(msg.getBytes("UTF8"))
    URL.encode(Base64.encode(digest))
  }

  //
  // Private members
  //
  private def s3ConstantForAccessPolicy(access: AccessPolicy): CannedAccessPolicy = {
    access match {
      case AccessPolicy.Public => CannedAccessPolicy.PUBLIC_READ
      case AccessPolicy.Private => CannedAccessPolicy.PRIVATE
    }
  }
}
