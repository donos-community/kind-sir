name := """kind-sir"""

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.4" % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

fork in run := false
