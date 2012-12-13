import sbt._
import Keys._

object MimerenderBuild extends Build {
  lazy val playVersionToScalaVersion = Map(
    "2.0" -> "2.9.1",
    "2.0.1" -> "2.9.1",
    "2.0.2" -> "2.9.1",
    "2.0.3" -> "2.9.1",
    "2.0.4" -> "2.9.1",
    "2.1-RC1" -> "2.10.0-RC5"
  )

  lazy val playVersion = sys.env.get("PLAY_VERSION").getOrElse("2.0.4")

  lazy val scalaVersion_ = playVersionToScalaVersion(playVersion)

  lazy val playScalaVersion = scalaVersion_.split("\\.0")(0)

  lazy val dependencies = Seq(
    "play" % ("play_" + playScalaVersion) % playVersion,
    "play" % ("play-test_" + playScalaVersion) % playVersion,
    "commons-lang" % "commons-lang" % "2.6"
  )

  lazy val root = Project(id = "mimerender",
                          base = file(".")).settings(
    scalaVersion := scalaVersion_,
    version := "0.1",
    resolvers += "Typesafe repository" at
      "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies := dependencies
  )
}