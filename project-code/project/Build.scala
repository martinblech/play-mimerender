import sbt._
import Keys._

object MimerenderBuild extends Build {
  lazy val playVersionToScalaVersion = Map(
    "2.1-RC1" -> "2.10.0"
  ).withDefaultValue("2.9.1")

  lazy val playVersion = sys.env.get("PLAY_VERSION").getOrElse("[2.0.1, 2.1)")

  lazy val dependencies = Seq(
    "play" %% "play" % playVersion,
    "play" %% "play-test" % playVersion
  )

  lazy val root = Project("mimerender", file(".")).settings(
    scalaVersion := playVersionToScalaVersion(playVersion),
    version := "0.1.2",
    resolvers += "Typesafe repository" at
      "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies := dependencies
  )
}
