package utils

import models.{Order, Celebrity, Account, Customer, Product}
import com.stripe.model.Token
import com.stripe.Stripe
import libs.{Payment, Time}

/**
 * Renders saved copies of domain objects that satisfy all relational integrity
 * constraints.
 */
object TestData {
  def newSavedCustomer(): Customer = {
    val acct = Account(email=Time.now.toString).save()
    val cust = Customer().save()

    acct.copy(customerId=Some(cust.id)).save()

    cust
  }

  def newSavedCelebrity(): Celebrity = {
    val acct = Account(email="celebrity"+Time.now.toString).save()
    val celeb = Celebrity().save()

    acct.copy(celebrityId = Some(celeb.id)).save()

    celeb
  }

  def newSavedProduct(): Product = {
    newSavedCelebrity().newProduct.save()
  }

  def newSavedOrder(): Order = {
    val customer = newSavedCustomer()
    val product = newSavedProduct()

    customer.buy(product).save()
  }

  def newStripeToken(): Token = {
    import java.lang.Integer

    val defaultCardParams = new java.util.HashMap[String, Object]();
    val defaultChargeParams = new java.util.HashMap[String, Object]();

    defaultCardParams.put("number", "4242424242424242");
    defaultCardParams.put("exp_month", new Integer(12));
    defaultCardParams.put("exp_year", new Integer(2015))
    defaultCardParams.put("cvc", "123");
    defaultCardParams.put("name", "Java Bindings Cardholder");
    defaultCardParams.put("address_line1", "522 Ramona St");
    defaultCardParams.put("address_line2", "Palo Alto");
    defaultCardParams.put("address_zip", "94301");
    defaultCardParams.put("address_state", "CA");
    defaultCardParams.put("address_country", "USA");

    defaultChargeParams.put("amount", new Integer(100));
    defaultChargeParams.put("currency", "usd");
    defaultChargeParams.put("card", defaultCardParams);

    val token = Token.create(defaultChargeParams)

    token
  }
}