import sbt._
import Keys._
import PlayProject._

object MimeBuild extends Build {

  val appName = "mimerender"
  val appVersion = "0.1"

  val appDependencies = Seq(
    "commons-lang" % "commons-lang" % "2.6"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang=SCALA)
}
