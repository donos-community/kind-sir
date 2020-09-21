name := """kind-sir"""

version := "0.3"

scalaVersion := "2.12.10"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.6.1" % "test",
  "org.scalatest" %% "scalatest" % "3.1.0" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "0.13.4",
  "org.json4s" %% "json4s-jackson" % "3.6.7",
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

assemblyJarName in assembly := "kind_sir.jar"
assemblyOutputPath in assembly := new File("./kind_sir.jar")
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

fork in run := false
