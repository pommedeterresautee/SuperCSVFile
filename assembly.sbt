import sbtassembly.Plugin.AssemblyKeys
import AssemblyKeys._

assemblySettings

jarName in assembly := "SuperTaxLawyer.jar"

mainClass in assembly := Some("com.taj.supertaxlawyer.Main")


