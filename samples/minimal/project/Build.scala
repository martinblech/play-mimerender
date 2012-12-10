import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "mimerender-sample-minimal"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  ).dependsOn(file("../.."))

}
