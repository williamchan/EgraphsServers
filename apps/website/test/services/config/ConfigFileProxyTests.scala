package services.config

import utils._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.io.{File, FileFilter}
import com.typesafe.config.ConfigFactory
import services.config

@RunWith(classOf[JUnitRunner])
class ConfigFileProxyTests extends EgraphsUnitTest {
  "ConfigFileProxy" should "successfully instantiate against all configurations" in {
    println("Verifying configuration: ")
    for (configFile <- this.configFiles) {
      print("\t" + configFile.getName)
      val typesafeConfig = ConfigFactory.parseFileAnySyntax(configFile)
      val playConfig = play.api.Configuration(typesafeConfig)

      try {
        new ConfigFileProxy(playConfig) // This will throw an exception if it fails
        println("...looks good.")
      } catch { 
        case e: IllegalArgumentException => 
          println("...FAILED.")
          throw e 
      }
    }
    println("")
  }

  //
  // Private members
  //
  private def configFiles = {
    val appConf = EgraphsUnitTest.resourceFile("application.conf")
    val confDir = appConf.getParentFile
    
    confDir.listFiles(new FileFilter {
      override def accept(file: File): Boolean = {
        Seq(".conf", ".properties", ".json").foldLeft(false) { (accum, next) => accum || file.getName.endsWith(next) }
      }
    })
  }
}