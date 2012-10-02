package services.db

import uk.me.lings.scalaguice.ScalaModule
import java.sql.Connection
import com.google.inject.{Singleton, AbstractModule}
import org.squeryl.Session

/**
 * Installs application bindings that relate to database usage.
 */
object DBModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[Schema].in[Singleton]

    // Connection factories: () => Connection

    // Non-annotated factory: gives a new connection from the pool
    bind[() => Connection].toInstance(() => play.db.DB.getConnection())

    // @CurrentTransaction factory: gives the one used in the current Squeryl session.
    // This will bork if there is no current session.
    bind[() => Connection]
      .annotatedWith[CurrentTransaction]
      .toInstance(() => Session.currentSession.connection)
  }
}