package services.config

import play.api.Configuration
import com.google.inject.Inject

/**
 * Proxies the running application.conf. Ensures that all necessary configuration values are present
 * and exposes an easy api for accessing them.
 **/
class ConfigFileProxy @Inject() (protected val playConfig: Configuration) extends ConfigPropertyAccessors {
  val applicationName = string("application.name")
  val applicationId = string("application.id")
  val applicationMode = string("application.mode", "dev", "prod")
  val applicationSecret = string("application.secret")
  val applicationBaseUrl = string("application.baseUrl")
  val applicationHttpsOnly = boolean("application.httpsOnly")
  val adminToolsEnabled = string("admin.tools.enabled", "full", "restricted")  
  val paymentVendor = string("payment.vendor", "stripe", "stripetest", "yesmaam")
  val stripeKeyPublishable = string("stripe.key.publishable")
  val stripeKeySecret = string("stripe.key.secret")
  val dateFormat = string("date.format")
  val blobstoreVendor = string("blobstore.vendor", "filesystem", "s3")
  val blobstoreNamespace = string("blobstore.namespace")
  val staticResourcesBlobstoreNamespace = string("staticresources.blobstore.namespace")
  val s3Id = string("s3.id")
  val s3Secret = string("s3.secret")
  val blobstoreAllowScrub = boolean("blobstore.allowscrub")
  val cdnContentUrl = playConfig.getString("cdn.contenturl")
  val dbDefaultAllowScrub = boolean("db.default.allowscrub")
  val dbDefaultUrl = string("db.default.url")
  val dbDefaultDriver = string("db.default.driver")
  val dbDefaultUser = string("db.default.user")
  val dbDefaultPassword = string("db.default.pass")
  val dbDefaultPoolTimeout = int("db.default.pool.timeout")
  val dbDefaultPoolMaxSize = int("db.default.pool.maxSize")
  val dbDefaultPoolMinSize = int("db.default.pool.minSize")
  val dbDefaultPoolMaxIdleTimeExcessConnections = int("db.default.pool.maxIdleTimeExcessConnections")

  val evolutionplugin = string("evolutionplugin", "enabled", "disabled")
  
  val cloudBeesApplicationId = if (applicationMode == "prod") { string("cloudbees.applicationId") }
  val cloudBeesApiKey = string("bees.api.key")
  val cloudBeesApiSecret = string("bees.api.secret")
  val cloudBeesApiDomain = string("bees.api.domain")
  val cloudBeesProjectAppDomain = string("bees.project.app.domain")

  val smtpMock = boolean("smtp.mock")
  val smtpOption = if(smtpMock) None else Some(new AnyRef {
    val smtpHost = string("smtp.host")
    val smtpPort = string("smtp.port")
    val smtpUser = string("smtp.user")
    val smtpPassword = string("smtp.password")
    val smtpSsl = string("smtp.ssl")
  })

  val mailBulkVendor = string("mail.bulk.vendor", "mock", "mailchimp")
  val mailBulkApikey = string("mail.bulk.apikey")
  val mailBulkDatacenter = string("mail.bulk.datacenter")
  val mailBulkNewsletterId = string("mail.bulk.newsletterid")

  val redisHost = string("redis.host")
  val redisPort = int("redis.port")
  val redisPassword = string("redis.password")

  val applicationCache = string("application.cache")

  val adminreviewSkip = boolean("adminreview.skip")
  val biometricsStatus = string("biometrics.status", "offline", "online")
  val signatureVendor = string("signature.vendor", "yesmaam", "xyzmoprod", "xyzmobeta")
  val voiceVendor = string("voice.vendor", "yesmaam", "fsprod", "fsbeta")

  val fbAppid = string("fb.appid")
  val fbAppsecret = string("fb.appsecret")
  val ipadBuildVersion = string("ipad.buildversion")
  val attachmentsPath = string("attachments.path")
  val parsersTextMaxlength = string("parsers.text.maxLength")
}