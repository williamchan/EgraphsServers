package services.http

import javax.net.ssl.{SSLContext, TrustManagerFactory}
import java.security.KeyStore
import play.api.Play.current

object SSLConfig {
  
  private val defaultSSLContext = SSLContext.getDefault()
  
  def enableCustomTrustore() {
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
    val keystoreStream = current.resourceAsStream("keystore/keystore").get
    keystore.load(keystoreStream, "catdoghouse".toCharArray())
    trustManagerFactory.init(keystore)
    val trustManagers = trustManagerFactory.getTrustManagers()
    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustManagers, null)
    SSLContext.setDefault(sc)
  }
  
  def disableCustomTrustore() {
    SSLContext.setDefault(defaultSSLContext)
  }

}