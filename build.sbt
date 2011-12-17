organization := "tv.cntt"

name         := "akka-cache"

version      := "1.0-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

resolvers += "Typesafe" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.3-RC4"

libraryDependencies += "se.scalablesolutions.akka" % "akka-remote" % "1.3-RC4"
