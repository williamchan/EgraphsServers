import java.util.{List, Date}
import javax.persistence.criteria.{Predicate, Root, CriteriaQuery, CriteriaBuilder}
import javax.persistence.Query
import models.Celebrity
import models.Product
import org.scalatest.BeforeAndAfterEach
import play.db.jpa.JPABase
import play.test._

import org.scalatest.matchers._

class BasicTests extends UnitFlatSpec with ShouldMatchers with BeforeAndAfterEach {

  override def beforeEach() {
    Fixtures.deleteDatabase()
  }

  it should "create Celebrity" in {
    var celebrity = buildCelebrity("Kevin Bacon", "kevin@bacon.com", "test1234", "Six degrees from me")

    celebrity = celebrity.refresh()
    //    celebrity.created should be(now) // figure out testing of Dates
    //    celebrity.updated should be(now)
    celebrity.name should be("Kevin Bacon")
    celebrity.email should be("kevin@bacon.com")
    celebrity.password = "test1234"
    celebrity.description = "Six degrees from me"
  }

  it should "find created Celebrity" in {
    val celebrity = buildCelebrity("Kevin Bacon", "kevin@bacon.com", "test1234", "Six degrees from me")

    var result = JPABase.em().find(classOf[Celebrity], celebrity.getId())
    //    result.created should be(now)
    //    result.updated should be(now)
    result.name should be("Kevin Bacon")
    result.email should be("kevin@bacon.com")
    result.password = "test1234"
    result.description = "Six degrees from me"
  }

  it should "update Celebrity" in {
    val celebrity = buildCelebrity("Kevin Bacon", "kevin@bacon.com", "test1234", "Six degrees from me")

    celebrity.name = "herp derp"
    celebrity.save()

    var result = JPABase.em().find(classOf[Celebrity], celebrity.getId())
    result.name should be("herp derp")
  }

  it should "delete Celebrity" in {
    val celebrity = buildCelebrity("Kevin Bacon", "kevin@bacon.com", "test1234", "Six degrees from me")

    var result = JPABase.em().find(classOf[Celebrity], celebrity.getId())
    result should not be (null)
    result.delete()
    result = JPABase.em().find(classOf[Celebrity], celebrity.getId())
    result should be(null)
  }

  it should "create Product" in {
    val celebrity = buildCelebrity("Kevin Bacon", "kevin@bacon.com", "test1234", "Six degrees from me")
    var product = buildProduct("autograph0", 20.00, celebrity)

    product = product.refresh()
    product.name should be("autograph0")
    product.price should be(20.00)
    product.celebrity should be(celebrity)
  }

  it should "update Product" in {
    val celebrity0 = buildCelebrity("Kevin Bacon", "kevin@bacon.com", "test1234", "Six degrees from me")
    var product = buildProduct("autograph0", 20.00, celebrity0)

    product.name = "autograph1"
    product.price = 30.00
    val celebrity1 = buildCelebrity("herp2", "herp2@bacon.com", "test1234", "derpderp")
    product.celebrity = celebrity1
    product.save()

    product.name should be("autograph1")
    product.price should be(30.00)
    product.celebrity should be(celebrity1)
  }

  it should "delete Product" in {
    val celebrity = buildCelebrity("Kevin Bacon", "kevin@bacon.com", "test1234", "Six degrees from me")
    var product = buildProduct("autograph0", 20.00, celebrity)

    var result = JPABase.em().find(classOf[Product], product.getId())
    result should not be (null)
    result.delete()
    result = JPABase.em().find(classOf[Product], product.getId())
    result should be(null)
  }

  it should "Learning JPA API" in {
    buildCelebrity("Kevin Bacon", "kevin@bacon.com", "test1234", "Six degrees from me")
    buildCelebrity("Kevin Bacon", "kevin@bacon.com1", "test1234", "Six degrees from me")
    buildCelebrity("Kevin Bacon", "kevin@bacon.com2", "test1234", "Six degrees from me")

    val query0: Query = JPABase.em().createQuery("SELECT c FROM Celebrity c WHERE name = 'Kevin Bacon'")
    val resultList0: List[Celebrity] = query0.getResultList.asInstanceOf[List[Celebrity]]
    resultList0.size() should be(3)
    val result0 = resultList0.get(0)
    //    query0.getFirstResult // Query.getFirstResult... see http://www.developer.com/java/ent/the-jpa-2-enhancements-every-java-developer-should-know.html
    result0.name should be("Kevin Bacon")

    val criteriaBuilder: CriteriaBuilder = JPABase.em().getCriteriaBuilder
    val criteriaQuery: CriteriaQuery[AnyRef] = criteriaBuilder.createQuery()
    val queryRoot: Root[Celebrity] = criteriaQuery.from(classOf[Celebrity])
    criteriaQuery.select(queryRoot)
    val predicate0: Predicate = criteriaBuilder.equal(queryRoot.get("name"), "Kevin Bacon")
    val predicate1: Predicate = criteriaBuilder.equal(queryRoot.get("email"), "kevin@bacon.com")
    criteriaQuery.where(Seq(predicate0, predicate1): _*)
    val resultList1: List[Celebrity] = JPABase.em().createQuery(criteriaQuery).getResultList.asInstanceOf[List[Celebrity]]
    resultList1.size() should be(1)
    val result1 = resultList1.get(0)
    result1.name should be("Kevin Bacon")
  }

  // --------------------------- PRIVATE

  private def buildCelebrity(name: String, email: String, password: String, description: String): Celebrity = {
    val celebrity = new Celebrity()
    celebrity.created = new Date()
    celebrity.updated = new Date()
    celebrity.name = name
    celebrity.email = email
    celebrity.password = password
    celebrity.description = description
    celebrity.save()
    celebrity
  }

  private def buildProduct(name: String, price: Double, celebrity: Celebrity): Product = {
    val product = new Product()
    product.name = name
    product.price = price
    product.celebrity = celebrity
    product.save()
    product
  }


}