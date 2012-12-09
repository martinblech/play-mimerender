import sbt._
import Keys._
import play.Project._

object MimeBuild extends Build {

  val version = "0.1"

  val sourceDependencies = Seq(
    jdbc,
    anorm,
    "commons-lang" % "commons-lang" % "2.6"
  )

  lazy val source = play.Project( name = "mimerender",
                                  path = file("source"),
                                  applicationVersion = version,
                                  dependencies = sourceDependencies)

  lazy val sample = play.Project( name = "mimerender-sample",
                                  path = file("samples/minimal")) dependsOn(source)
}
