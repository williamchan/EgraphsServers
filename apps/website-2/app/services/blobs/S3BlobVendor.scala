package services.blobs

import play.api.Play.configuration
import org.jclouds.blobstore.BlobStoreContextFactory
import org.jclouds.aws.s3.AWSS3Client
import org.jclouds.s3.domain.CannedAccessPolicy
import services.logging.Logging
import services.AppConfig
import services.http.HttpContentService
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.net.URLEncoder
import play.libs.Codec

/** [[services.blobs.Blobs.BlobProvider]] implementation backed by Amazon S3 */
private[blobs] case class S3BlobVendor(
  s3id: String,
  s3secret: String
) extends BlobVendor with Logging {

  val httpContent = AppConfig.instance[HttpContentService]

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

  override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
    import org.jclouds.s3.options.PutObjectOptions.Builder.withAcl

    val s3:AWSS3Client = context.getProviderSpecificContext.getApi

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

    // Ship it off
    s3.putObject(namespace, s3Object, withAcl(s3ConstantForAccessPolicy(access)))
  }

  override def checkConfiguration() {
    require(
      s3id != null,
      """
      application.conf: An "s3.id" configuration must be provided.
      """
    )
    require(
      s3secret != null,
      """
      application.conf: An "s3.secret" configuration must be provided.
      """
    )
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
    URLEncoder.encode(Codec.encodeBASE64(digest), "UTF-8")
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

/** [[services.blobs.Blobs.BlobProvider]] implementation backed by Amazon S3 */
private[blobs] object S3BlobVendor
  extends S3BlobVendor(configuration.getProperty("s3.id"), configuration.getProperty("s3.secret"))
