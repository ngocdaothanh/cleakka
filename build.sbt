organization := "tv.cntt"

name         := "cleakka"

version      := "1.0-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

libraryDependencies += "com.twitter" %% "chill" % "0.2.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2-M3"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster-experimental" % "2.2-M3"

libraryDependencies += "org.specs2" %% "specs2" % "1.14" % "test"
