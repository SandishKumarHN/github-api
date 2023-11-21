ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.12"

organization := "org.openorg.github"

fork := true

lazy val dependencies = Seq(
  "org.scalikejdbc" %% "scalikejdbc" % "4.1.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  "com.typesafe" % "config" % "1.4.3",
  "net.liftweb" %% "lift-json" % "3.5.0",
  "org.apache.httpcomponents" % "httpclient" % "4.5.14",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "org.xerial" % "sqlite-jdbc" % "3.44.0.0",
  "com.typesafe.akka" %% "akka-http" % "10.6.0-M1",
  "com.typesafe.akka" %% "akka-actor" % "2.9.0-M2",
  "com.typesafe.akka" %% "akka-stream" % "2.9.0-M2",
  "com.lihaoyi" %% "upickle" % "3.1.3",
  "org.json4s" %% "json4s-jackson" % "4.1.0-M3",
  "io.circe" %% "circe-core" % "0.15.0-M1",
  "io.circe" %% "circe-parser" % "0.15.0-M1",
  "io.circe" %% "circe-generic" % "0.15.0-M1",
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "org.mockito" % "mockito-core" % "3.12.4" % Test,
  "org.scalatest" %% "scalatest" % "3.3.0-SNAP4" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.6.0-M1" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.9.0-M2" % Test
)

lazy val root = (project in file("."))
  .settings(
    name := "github-api",
    libraryDependencies ++= dependencies
  )
