organization := "tv.cntt"

name         := "cleanerakka"

version      := "1.0-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster-experimental" % "2.1.2"
