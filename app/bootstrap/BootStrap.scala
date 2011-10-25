package bootstrap

import play.db.jpa.JPABase
import play.jobs._
import java.util.Date
import com.stripe.Stripe

@OnApplicationStart class BootStrap extends Job {

  override def doJob(): Unit = {

    Stripe.apiKey = "pvESi1GjhD9e8RFQQPfeH8mHZ2GIyqQV"

    import models._
    // Import initial data if the database is empty

    ""

    // for JPA
    var kevin = JPABase.em().find(classOf[Celebrity], 1L)
    if (kevin == null) {
      println("Creating Kevin Bacon...")
      kevin = new Celebrity()
      kevin.created = new Date()
      kevin.updated = new Date()
      kevin.name = "Kevin Bacon"
      kevin.email = "kevin@bacon.com"
      kevin.password = "test1234"
      kevin.description = "Six degrees from me"
      kevin.save()
      println("Creating Kevin Bacon products...")
      val product0 = new Product()
      product0.created = new Date()
      product0.updated = new Date()
      product0.celebrity = kevin
      product0.name = "autograph0"
      product0.price = 5.00
      product0.save()
      val product1 = new Product()
      product1.created = new Date()
      product1.updated = new Date()
      product1.celebrity = kevin
      product1.name = "autograph1"
      product1.price = 10.00
      product1.save()
      println("Done creating Kevin Bacon sample data")

    }
  }

}
