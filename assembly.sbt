import sbtassembly.Plugin._
import AssemblyKeys._

assemblySettings

jarName in assembly := { s"${name.value}_v${version.value}.jar" }

mainClass in assembly := Some("com.TAJ.SuperCSVFile.Main")


