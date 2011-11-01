import org.scalatest.matchers.ShouldMatchers
import org.squeryl.adapters.{MySQLAdapter, PostgreSqlAdapter, H2Adapter}
import play.test.UnitFlatSpec
import db.Adapter

class AdapterTests extends UnitFlatSpec with ShouldMatchers {

  "Adapter" should "be h2 for string 'mem'" in {
    Adapter.getForDbString("mem") should be theSameInstanceAs (Adapter.h2)
  }

  it should "be mysql for mysql url" in {
    val adapter = Adapter.getForDbString("mysql://herp.derp")
    adapter should be theSameInstanceAs (Adapter.mysql)
  }

  it should "be postgres for postgres url" in {
    val adapter = Adapter.getForDbString("postgresql://herp:derp/schmerp")

    adapter should be theSameInstanceAs (Adapter.postgres)
  }

  it should "otherwise throw an illegal argument exception" in {
    evaluating { Adapter.getForDbString("oracle://dur") } should produce [IllegalArgumentException]
  }

  "The lazy adapters" should "all be of the correct types" in  {
    Adapter.h2.isInstanceOf[H2Adapter] should be (true)
    Adapter.mysql.isInstanceOf[MySQLAdapter] should be (true)
    Adapter.postgres.isInstanceOf[PostgreSqlAdapter] should be (true)
  }
}