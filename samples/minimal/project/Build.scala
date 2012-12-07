import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "minimal"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "play-mimerender" %% "play-mimerender" % "1.0-SNAPSHOT"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
