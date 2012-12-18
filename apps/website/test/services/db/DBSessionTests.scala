package services.db

import models.{AccountStore, Account}
import utils.{TestData, ClearsCacheBefore, EgraphsUnitTest}
import org.squeryl.Session
import akka.actor.Actor
import akka.dispatch.Dispatchers
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import services.logging.Logging
import services.AppConfig
import java.sql.Connection
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DBSessionTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with Logging
{
  private def underTest: (DBSession, Connection) = {
    val connection = mock[Connection]
    (new DBSession(() => connection), connection)
  }

  "A call to DBSession.connected" should "return the 'continue' value, commit, and close" in new EgraphsTestApplication {
    val (dbSession, connection) = underTest

    val returned = dbSession.connected(TransactionSerializable) { 12345 }

    returned should be (12345)

    there was one (connection).commit()
    there was one (connection).close()
    there was no (connection).rollback()
  }

  it should "rollback and close when an exception is thrown by the continue block" in new EgraphsTestApplication {
    val (dbSession, connection) = underTest
    
    evaluating { 
      dbSession.connected(TransactionSerializable) { throw new RuntimeException() } 
    } should produce [RuntimeException]

    there was one (connection).rollback()
    there was one (connection).close()
    there was no (connection).commit()
  }

  it should "rollback and close when committing throws an exception" in new EgraphsTestApplication {
    val (dbSession, connection) = underTest

    connection.commit() throws new RuntimeException()

    evaluating {
      dbSession.connected(TransactionSerializable) { "Hello world" }
    } should produce[RuntimeException]

    there was one (connection).commit()
    there was one (connection).rollback()
    there was one (connection).close()
  }

  it should "set the correct transaction isolation level to the database" in new EgraphsTestApplication {
    val (dbSession, connection) = underTest
    
    dbSession.connected(TransactionSerializable) { "Hello World" }

    there was one (connection).setTransactionIsolation(TransactionSerializable.jdbcIsolationLevel)
  }

  it should "Save and recall an Account correctly both within and between transactions" in new EgraphsTestApplication {
    val dbSession = AppConfig.instance[DBSession]

    val emailAddress = TestData.generateEmail("herpyderpson", "derp.org")
    val savedAccount = dbSession.connected(TransactionSerializable) {
      val account = Account(email=emailAddress)

      account.id should be (0)
      val saved = account.save()
      saved.id should not be (0)
      saved
    }

    dbSession.connected(TransactionSerializable) {
      val restored = AppConfig.instance[AccountStore].findById(savedAccount.id)
      restored should not be (None)
      restored.get.email should be (emailAddress)
    }
  }

  it should "respect Serializable constraints" in new EgraphsTestApplication {
    val dbSession = AppConfig.instance[DBSession]
    val accountStore = AppConfig.instance[AccountStore]

    // Insert and commit an Account
    val emailAddress = TestData.generateEmail("originalAddress", "derp.org")
    val account: Account = dbSession.connected(TransactionSerializable) {
      Account(email=emailAddress).save()
    }

    // Prepare an actor that will modify the account name
    val actorEmail = TestData.generateEmail("actorEmail", "derp.org")
    val mainThreadEmail = TestData.generateEmail("mainThreadEmail", "derp.org")

    // Create two different sessions that both alter the same element
    val thrown = evaluating {
      dbSession.connected(TransactionSerializable) {
        val outerAccount = accountStore.get(account.id)
        // Inner transaction
        dbSession.connected(TransactionSerializable) {
          accountStore.get(account.id).copy(email=mainThreadEmail).save()
        }

        outerAccount.copy(email=actorEmail).save()
      }
    } should produce [RuntimeException]

    thrown.getMessage should include ("40001") // Access due to concurrent update
 }

  it should "successfully persist data if the transaction is not readOnly" in new EgraphsTestApplication {
    val dbSession = AppConfig.instance[DBSession]
    val emailAddress = TestData.generateEmail("herpyderpson", "derp.org")
    val account = dbSession.connected(TransactionSerializable, readOnly = false) {
      Account(email = emailAddress).save()
    }
    account should not be (null)
  }

  it should "respect when a transaction is readOnly and throw an exception if a write is attempted" in new EgraphsTestApplication {
    val dbSession = AppConfig.instance[DBSession]
    val emailAddress = TestData.generateEmail("herpyderpson", "derp.org")
    val thrown1 = evaluating {
      dbSession.connected(TransactionSerializable, readOnly = true) {
        Account(email = emailAddress).save()
      }
    } should produce[RuntimeException]
    thrown1.getMessage should include("Exception while executing statement : ERROR: cannot execute INSERT in a read-only transaction")

    val account = dbSession.connected(TransactionSerializable, readOnly = false) {
      Account(email = emailAddress).save()
    }
    val thrown2 = evaluating {
      dbSession.connected(TransactionSerializable, readOnly = true) {
        account.copy(email = "a" + emailAddress).save()
      }
    } should produce[RuntimeException]
    thrown2.getMessage should include("Exception while executing statement : ERROR: cannot execute UPDATE in a read-only transaction")
  }
}
