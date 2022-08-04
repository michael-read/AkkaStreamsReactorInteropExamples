import CommonForBuild._
import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "Example Org"

lazy val root =
  (project in file("."))
    .settings(name := "AkkaStreamsReactorInteropExamples")
    .settings(commonSettings)
    .aggregate(simpleExample)

lazy val simpleExample =
  (project in file("simple-example"))
    .settings(name := "SimpleExamples", libraryDependencies ++= allDeps)
    .settings(commonSettings)