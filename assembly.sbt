import sbtassembly.Plugin.AssemblyKeys
import AssemblyKeys._

assemblySettings

jarName in assembly := "SuperCSVFile.jar"

mainClass in assembly := Some("com.taj.supercsvfile.Main")


