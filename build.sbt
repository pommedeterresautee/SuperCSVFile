name := "Super Tax Lawyer"

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
  "com.ibm.icu" % "icu4j" % "52.1",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "commons-codec" % "commons-codec" % "1.7" % "test",
  "org.rogach" %% "scallop" % "0.9.5"
)