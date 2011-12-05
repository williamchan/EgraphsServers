package db

import org.scalatest.matchers.ShouldMatchers
import org.squeryl.adapters.{MySQLAdapter, PostgreSqlAdapter, H2Adapter}
import play.test.UnitFlatSpec

class DBAdapterTests extends UnitFlatSpec with ShouldMatchers {

  "DBAdapter" should "be h2 for string 'mem'" in {
    DBAdapter.getForDbString("mem") should be theSameInstanceAs (DBAdapter.h2)
  }

  it should "be mysql for mysql url" in {
    val adapter = DBAdapter.getForDbString("mysql://herp.derp")
    adapter should be theSameInstanceAs (DBAdapter.mysql)
  }

  it should "be postgres for postgres url" in {
    val adapter = DBAdapter.getForDbString("postgresql://herp:derp/schmerp")

    adapter should be theSameInstanceAs (DBAdapter.postgres)
  }

  it should "otherwise throw an illegal argument exception" in {
    evaluating { DBAdapter.getForDbString("oracle://dur") } should produce [IllegalArgumentException]
  }

  "The lazy adapters" should "all be of the correct types" in  {
    DBAdapter.h2.isInstanceOf[H2Adapter] should be (true)
    DBAdapter.mysql.isInstanceOf[MySQLAdapter] should be (true)
    DBAdapter.postgres.isInstanceOf[PostgreSqlAdapter] should be (true)
  }
}