import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease._
import ReleaseStateTransformations._

name := "SuperCSVFile"

version := "1.0.4"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.1" % "test",
  "com.ibm.icu" % "icu4j" % "53.1",
  "com.typesafe" % "scalalogging-slf4j_2.10" % "1.1.0",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "org.scalatest" % "scalatest_2.10" % "2.1.3" % "test",
  "commons-codec" % "commons-codec" % "1.9" % "test",
  "org.rogach" %% "scallop" % "0.9.5"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:reflectiveCalls"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(CompactControlReadability, true)
  .setPreference(IndentLocalDefs, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
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