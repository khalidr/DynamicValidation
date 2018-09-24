name := "motiion"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions ++=
  Seq("-encoding", "UTF8", "-unchecked", "-feature", "-deprecation", "-language:postfixOps", "-language:implicitConversions", "-language:higherKinds")

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-http" % "10.1.5",
    "com.couchbase.client" % "java-client" % "2.6.2",
    "io.reactivex" %% "rxscala" % "0.26.5",
    "com.typesafe.play" %% "play-json" % "2.6.10",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    "de.heikoseeberger" %% "akka-http-play-json" % "1.21.0",
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.5" % Test,
    "org.typelevel" %% "cats-core" % "1.4.0"


  )
}