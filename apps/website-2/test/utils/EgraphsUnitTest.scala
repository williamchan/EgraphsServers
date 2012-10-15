package utils

import org.specs2.mock.Mockito
import play.mvc.Http.Request
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.mvc.Session
import com.typesafe.config.ConfigFactory
import play.api.test.FakeRequest
import play.api.Configuration
import play.api.test.FakeApplication
import play.api.test._
import play.api.test.Helpers._
import java.io.File

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
  
  trait TestApplication {
    implicit val app = EgraphsUnitTest.app
    def resourceFile(resource: String): File = EgraphsUnitTest.resourceFile(resource)
  }
}

object EgraphsUnitTest {
  private val classLoader = this.getClass.getClassLoader
  private lazy val typesafeConfig = ConfigFactory.load("local.conf")
  private lazy val playConfig = Configuration(typesafeConfig)
    
  lazy val app = {
    val theApp = new FakeApplication(path=resourceFile("local.conf").getParentFile) {
      override def configuration = playConfig
    }
    
    play.api.Play.start(theApp)
  }
  
  def resourceFile(resource: String): File = {
    classLoader.getResource(resource) match {
      case null => new File("")
      case someUrl => new File(someUrl.getFile)
    }
  }
}