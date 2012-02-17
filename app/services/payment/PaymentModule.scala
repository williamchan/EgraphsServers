package services.payment

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.AbstractModule

/**
 * Payment service configuration
 */
object PaymentModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[Payment].to[NicePayment]
  }
}