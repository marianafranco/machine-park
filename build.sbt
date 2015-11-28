name := """machine-park"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play23",
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "angularjs" % "1.4.8",
  "org.webjars" % "bootstrap" % "3.3.6"
)
