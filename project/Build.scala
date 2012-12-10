import sbt._
import Keys._
import play.Project._

object MimeBuild extends Build {

  val version = "0.1"

  val sourceDependencies = Seq(
    "commons-lang" % "commons-lang" % "2.6"
  )

  lazy val main = play.Project(name = "mimerender",
                               path = file("source"),
                               applicationVersion = version,
                               dependencies = sourceDependencies)
}
