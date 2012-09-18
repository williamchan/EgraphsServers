package services.db

import models.{AccountStore, Account}
import utils.{TestData, ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}
import org.squeryl.Session
import akka.actor.Actor
import akka.dispatch.Dispatchers
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import services.logging.Logging
import services.AppConfig
import java.sql.Connection

class DBSessionTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with Logging
{
  private def underTest: (DBSession, Connection) = {
    val connection = mock[Connection]
    (new DBSession(() => connection), connection)
  }

  "A call to DBSession.connected" should "return the 'continue' value, commit, and close" in {
    val (dbSession, connection) = underTest    

    val returned = dbSession.connected(TransactionSerializable) { 12345 }

    returned should be (12345)

    there was one (connection).commit()
    there was one (connection).close()
    there was no (connection).rollback()
  }

  it should "rollback and close when an exception is thrown by the continue block" in {
    val (dbSession, connection) = underTest
    
    evaluating { 
      dbSession.connected(TransactionSerializable) { throw new RuntimeException() } 
    } should produce [RuntimeException]

    there was one (connection).rollback()
    there was one (connection).close()
    there was no (connection).commit()
  }

  it should "rollback and close when committing throws an exception" in {
    val (dbSession, connection) = underTest

    connection.commit() throws new RuntimeException()

    evaluating {
      dbSession.connected(TransactionSerializable) { "Hello world" }
    } should produce[RuntimeException]

    there was one (connection).commit()
    there was one (connection).rollback()
    there was one (connection).close()
  }

  it should "set the correct transaction isolation level to the database" in {
    val (dbSession, connection) = underTest
    
    dbSession.connected(TransactionSerializable) { "Hello World" }

    there was one (connection).setTransactionIsolation(TransactionSerializable.jdbcIsolationLevel)
  }

  it should "Save and recall an Account correctly both within and between transactions" in {
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

  it should "respect Serializable constraints" in {
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

  it should "respect readOnly transactions" in {
    val dbSession = AppConfig.instance[DBSession]
    val emailAddress = TestData.generateEmail("herpyderpson", "derp.org")
    val thrown = evaluating {
      dbSession.connected(TransactionSerializable, readOnly = true) {
        Account(email = emailAddress).save()
      }
    } should produce[RuntimeException]
    thrown.getMessage should include("25006") // Invalid write during read-only transaction
  }
}

object DBSessionTestActors {
  case class SetAccountEmail(accountId: Long, email:String)

  class SerializableTestActor extends Actor with Logging {
    self.dispatcher = Dispatchers.newExecutorBasedEventDrivenDispatcher("serializabletest")
      .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(100)
      .setCorePoolSize(1)
      .setMaxPoolSize(1)
      .setKeepAliveTimeInMillis(60000)
      .setRejectionPolicy(new CallerRunsPolicy)
      .build

    val myDbSession = AppConfig.instance[DBSession]
    val myAccountStore = AppConfig.instance[AccountStore]

    def receive = {
      case SetAccountEmail(accountId, email) => {
        log("Worker thread: received message.")
        myDbSession.connected(TransactionSerializable) {
          println("Connection is "+Session.currentSession.connection)
          myAccountStore.get(accountId).copy(email=email).save()
        }
        log("Worker thread: committed.")
        self.reply("done")
      }
    }
  }
}