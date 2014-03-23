name := "Super Tax Lawyer"

version := "1.0.1"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.0",
  "com.ibm.icu" % "icu4j" % "52.1",
  "com.typesafe" % "scalalogging-slf4j_2.10" % "1.1.0",
  "org.slf4j" % "slf4j-simple" % "1.7.6",
  "org.scalatest" % "scalatest_2.10" % "2.1.2" % "test",
  "commons-codec" % "commons-codec" % "1.9" % "test",
  "org.rogach" %% "scallop" % "0.9.5"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:reflectiveCalls"
)