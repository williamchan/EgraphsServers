import sbt._
import Keys._
import play.core.PlayVersion
import play.Project._

object BuildHelpers {

  val currentPlayDependency = "play" % "play_2.1.0" % PlayVersion.current

  val defaultModuleSettings = Project.defaultSettings ++ Seq(
    scalaVersion := "2.10.0",

    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    ),
    
    libraryDependencies ++= Seq(
      "play" %% "play" % PlayVersion.current,
    
      "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
      "play" %% "play-test" % PlayVersion.current % "test"
    ),

    shellPrompt := playPrompt,

    commands ++= Seq(playCommand)
  )

  /** Adds conf/ to the generated classpath. */
  def playEclipseClasspathAdditions = {
    import com.typesafe.sbteclipse.core._
    import com.typesafe.sbteclipse.core.EclipsePlugin._
    import com.typesafe.sbteclipse.core.Validation
    import scala.xml._
    import scala.xml.transform.RewriteRule
    
    new EclipseTransformerFactory[RewriteRule] {
      override def createTransformer(ref: ProjectRef, state: State): Validation[RewriteRule] = {
        setting(crossTarget in ref, state) map { ct =>
          new RewriteRule {
            override def transform(node: Node): Seq[Node] = node match {
              case elem if (elem.label == "classpath") =>
                val newChild = elem.child ++ <classpathentry path="conf" kind="src"></classpathentry>                  
                Elem(elem.prefix, "classpath", elem.attributes, elem.scope, newChild: _*)
             case other =>
                other  
            }
          }
        }
      }
    }
  }

  object ModuleProject {
    def apply(name: String, settings: Seq[Setting[_]] = Seq.empty[Setting[_]]): Project = {      
      Project(
        id=name,
        base=file("modules") / name,
        settings=defaultModuleSettings ++ settings
      )
    }
  }
}