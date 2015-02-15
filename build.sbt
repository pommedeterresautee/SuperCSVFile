import com.typesafe.sbt.SbtScalariform._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease._

import scalariform.formatter.preferences._

name := "SuperCSVFile"

version := "1.0.9"

scalaVersion := "2.11.5"

 resolvers ++= Seq(
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
 )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.ibm.icu" % "icu4j" % "54.1.1",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.slf4j" % "slf4j-simple" % "1.7.10",
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "org.rogach" %% "scallop" % "0.9.5",
  "com.github.nscala-time" %% "nscala-time" % "1.8.0",
  "com.chuusai" %% "shapeless" % "2.0.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.9" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "commons-codec" % "commons-codec" % "1.9" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:reflectiveCalls",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-nullary-unit",
  "-Ywarn-nullary-override",
  "-Ywarn-value-discard"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignParameters, true)
  .setPreference(CompactControlReadability, true)
  .setPreference(IndentLocalDefs, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)

net.virtualvoid.sbt.graph.Plugin.graphSettings

releaseSettings

useGlobalVersion := false

exportJars := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

incOptions := incOptions.value.withNameHashing(nameHashing = true)

assemblyJarName in assembly := { s"${name.value}_v${version.value}.jar" }

mainClass in assembly := Some("com.TAJ.SuperCSVFile.Main")

lazy val csv4s = RootProject(uri("https://github.com/pommedeterresautee/CSV4S.git"))

lazy val root = project in file(".") dependsOn csv4s
