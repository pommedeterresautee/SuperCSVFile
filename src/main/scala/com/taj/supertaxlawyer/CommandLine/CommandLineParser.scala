package com.taj.supertaxlawyer.CommandLine

import org.rogach.scallop.ScallopConf
import java.io.File
import java.nio.charset.Charset
import scalaz._
import Scalaz._

object CommandLineParser {
  def apply (args: Array[String]) = new CommandLineParser(args)
}

/**
 * Parser of command line arguments.
 * @param args arguments provided through the command line.
 */
class CommandLineParser(args: Array[String]) extends ScallopConf(args) {
    banner( """
                  | ____                          _____            _
                  |/ ___| _   _ _ __   ___ _ __  |_   _|_ ___  __ | |    __ ___      ___   _  ___ _ __
                  |\___ \| | | | '_ \ / _ \ '__|   | |/ _` \ \/ / | |   / _` \ \ /\ / / | | |/ _ \ '__|
                  | ___) | |_| | |_) |  __/ |      | | (_| |>  <  | |__| (_| |\ V  V /| |_| |  __/ |
                  ||____/ \__,_| .__/ \___|_|      |_|\__,_/_/\_\ |_____\__,_| \_/\_/  \__, |\___|_|
                  |            |_|                                                     |___/
                  |		""".stripMargin + s"""

    Super Tax Lawyer is a program to play with accounting exported as text files.
     """)
    val fileExist: String => Boolean = new File(_).exists()
    val fileListExist: List[String] => Boolean = _.forall(fileExist)
    val columnSize = opt[String]("columnSize", descr = "Print the detected encoding of each file provided.", validate = fileExist)
    val splitter = opt[String]("splitter", descr = "Character used to split a line in columns. Use TAB for tabulation and SPACE for space separators.")
    val encoding = opt[String]("forceEncoding", descr = "Force the encoding of the text file.", validate = Charset.isSupported)
    val columnCount = opt[Int]("columnCount", descr = "[OPTIONAL] Number of columns expected.")
    val excludeTitles = toggle("excludeTitles", descrYes = "Exclude titles of columns in column size result.", default = false.some, prefix = "no-")
    val output = opt[String]("outputFolder", descr = "Path to the folder where to save the results.", validate = new File(_).isDirectory)
    val debug = toggle("debug", descrYes = "Display lots of debug information during the process.", descrNo = "Display minimum during the process (same as not using this argument).", default = false.some, prefix = "no-")
    val help = opt[Boolean]("help", descr = "Show this message.")
    conflicts(columnSize, List(help))
}