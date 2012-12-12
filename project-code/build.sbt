name := "mimeparse"

version := "0.1"

scalaVersion := "2.9.1"

resolvers += "Typesafe repository" at
  "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq("play" %% "play" % "2.0.4",
                            "play" %% "play-test" % "2.0.4",
                            "commons-lang" % "commons-lang" % "2.6")
