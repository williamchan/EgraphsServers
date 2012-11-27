package utils

import org.specs2.mock.Mockito
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.mvc.Session
import com.typesafe.config.ConfigFactory
import play.api.Play
import play.api.test.FakeRequest
import play.api.Configuration
import play.api.test.FakeApplication
import play.api.Application
import play.api.test._
import play.api.test.Helpers._
import java.io.File
import services.logging.Logging
import services.mvc.celebrity.CatalogStarsTestUtil

/**
 * Convenience method provides most generally used traits for a scalatest
 */
trait EgraphsUnitTest extends FlatSpec
  with ShouldMatchers
  with Mockito 
{
  
  def newRequestAndMockSession: (FakeRequest[_], Session) = {
    (FakeRequest(), mock[Session])
  }
  
  trait EgraphsTestApplication {
    implicit val app: Application = EgraphsUnitTest.runApp()
    def resourceFile(resource: String): File = EgraphsUnitTest.resourceFile(resource)
  }
}

object EgraphsUnitTest extends Logging {
  private val classLoader = this.getClass.getClassLoader
  private lazy val typesafeConfig = ConfigFactory.load("local.conf")
  private val playConfig = Configuration(typesafeConfig)

  def runApp(): Application = {
    // Only start the app if it wasn't already running
    Play.maybeApplication.filter(runningApp => runningApp == testApp).getOrElse {
      log("Starting the test app with root in " + testApp.path) 
      Play.start(testApp)

      testApp
    }
  }
  
  def resourceFile(resource: String): File = {
    classLoader.getResource(resource) match {
      case null => new File("")
      case someUrl => new File(someUrl.getFile)
    }
  }
  
  lazy val testApp = {
    new FakeApplication(path=appRoot) {
      override def configuration = playConfig
    }
  }
  
  private lazy val appRoot: File = {
    def recursivelyFindProjectRoot(classpathFile: File): File = {
      if (new File(classpathFile, "conf").exists()) {
        classpathFile 
      } else {
        recursivelyFindProjectRoot(classpathFile.getParentFile)
      }
    }

    val root = recursivelyFindProjectRoot(resourceFile("local.conf"))

    root
  }
}