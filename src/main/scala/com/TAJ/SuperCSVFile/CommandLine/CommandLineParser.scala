/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014. TAJ - Société d'avocats
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * EXCEPT AS CONTAINED IN THIS NOTICE, THE NAME OF TAJ - Société d'avocats SHALL
 * NOT BE USED IN ADVERTISING OR OTHERWISE TO PROMOTE THE SALE, USE OR OTHER
 * DEALINGS IN THIS SOFTWARE WITHOUT PRIOR WRITTEN AUTHORIZATION FROM
 * TAJ - Société d'avocats.
 */ package com.TAJ.SuperCSVFile.CommandLine

import org.rogach.scallop.ScallopConf
import java.io.File
import java.nio.charset.Charset
import scalaz._
import Scalaz._

object CommandLineParser {
  def apply(args: Array[String]) = new CommandLineParser(args)
  val fileExist: String ⇒ Boolean = new File(_).isFile
  val parentFolderExists: String ⇒ Boolean = path ⇒ !new File(path).isDirectory && new File(path).getParentFile.isDirectory
  val fileListExist: List[String] ⇒ Boolean = _.forall(fileExist)
}

/**
 * Parser of command line arguments.
 * @param args arguments provided through the command line.
 */
class CommandLineParser(args: Array[String]) extends ScallopConf(args) {
  import CommandLineParser._
  banner("""
           | ____                           ____ ______     __  _____ _ _
           |/ ___| _   _ _ __   ___ _ __   / ___/ ___\ \   / / |  ___(_) | ___
           |\___ \| | | | '_ \ / _ \ '__| | |   \___ \\ \ / /  | |_  | | |/ _ \
           | ___) | |_| | |_) |  __/ |    | |___ ___) |\ V /   |  _| | | |  __/
           ||____/ \__,_| .__/ \___|_|     \____|____/  \_/    |_|   |_|_|\___|
           |            |_|
           |		""".stripMargin + s"""

    Super CSV File is a program to play with accounting exported as text files.
     """)
  footer("\nThis application has been brought to you by Taj - Société d'avocats.")

  version(s"Super CSV File - version ${com.TAJ.SuperCSVFile.Main.getClass.getPackage.getImplementationVersion}")

  val inputFiles = opt[String]("inputFile", descr = "Path to the file to analyze.", validate = fileExist)

  val setEncoding = opt[String]("encoding", descr = "[OPTIONAL INFO] Force the encoding of the input file.", validate = Charset.isSupported)

  val columnSize = opt[Boolean]("columnSize", descr = "Print the detected encoding of each file provided.")
  val columnCount = opt[Int]("columnCount", descr = "[OPTIONAL INFO] Number of columns expected.")
  val setSplitter = opt[String]("setSplitter", descr = "[OPTIONAL INFO] Character used to split a line in columns. Use TAB for tabulation and SPACE for space separators.")
  val excludeTitles = opt[Boolean]("titlesExcluded", descr = "Exclude titles of columns in column size result.", default = false.some)
  val outputDelimiter = opt[String]("delimiterOutput", descr = "Path to the file where to save the column delimiter.", validate = parentFolderExists, noshort = true)
  val outputColumnSizes = opt[String]("columnOutput", descr = "Path to the file where to save the sizes of columns of the analyzed file.", validate = parentFolderExists, noshort = true)

  val extract = opt[Boolean]("extract", descr = "Extract specific block of lines from the file. Need to precise the number of lines to extract from the text file.")
  val startLine = opt[Int]("firstLine", descr = "[OPTIONAL INFO] Set the first line for the selection. WARNING: Start at 0 and not 1. This line is included in the selection.", validate = _ >= 0)
  val lastLine = opt[Int]("lastLine", descr = "[OPTIONAL INFO] Set the last line for the selection. This line is included in the selection.", validate = _ >= 0)
  val outputExtractLines = opt[String]("extractLinesOutput", descr = "Path to the file where to save the lines extracted from the file.", validate = parentFolderExists, noshort = true)

  val linesCount = opt[Boolean]("linesCount", descr = "Count the lines in the input file.")
  val outputLinesCount = opt[String]("linesCountOutput", descr = "Path to the file where to save the number of lines of the analyzed file.", validate = parentFolderExists, noshort = true)

  val debug = opt[Boolean]("debug", descr = "Display lots of debug information during the process.", default = false.some, noshort = true)
  val help = opt[Boolean]("help", descr = "Show this message.")
  val version = opt[Boolean]("version", descr = "Show the application.")

  dependsOnAll(setSplitter, List(columnSize))
  dependsOnAll(columnCount, List(columnSize))
  dependsOnAll(outputExtractLines, List(extract))
  dependsOnAll(outputLinesCount, List(linesCount))
  dependsOnAll(outputDelimiter, List(columnSize))
  dependsOnAll(outputColumnSizes, List(columnSize))
  mutuallyExclusive(outputDelimiter, setSplitter)
  dependsOnAll(excludeTitles, List(startLine))

  validateOpt(inputFiles) {
    case Some(path) ⇒ Right(Unit)
    case None       ⇒ Left("Provide an input file!")
  }

  validateOpt(columnSize, extract, linesCount) {
    case (None, None, None) ⇒ Left("Provide an action minimum!")
    case _                  ⇒ Right(Unit)
  }
}