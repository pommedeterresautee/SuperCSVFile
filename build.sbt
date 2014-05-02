import com.typesafe.sbt.SbtScalariform._
import sbtassembly.Plugin._
import sbtassembly.Plugin.AssemblyKeys._
import scala.Some
import scalariform.formatter.preferences._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease._
import ReleaseStateTransformations._

name := "SuperCSVFile"

version := "1.0.6"

scalaVersion := "2.11.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.2",
  "com.ibm.icu" % "icu4j" % "53.1",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "org.rogach" %% "scallop" % "0.9.5",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "com.github.nscala-time" %% "nscala-time" % "1.0.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.2" % "test",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test",
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

assemblySettings

jarName in assembly := { s"${name.value}_v${version.value}.jar" }

mainClass in assembly := Some("com.TAJ.SuperCSVFile.Main")