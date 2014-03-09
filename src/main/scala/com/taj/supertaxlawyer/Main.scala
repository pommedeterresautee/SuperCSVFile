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
 */

package com.taj.supertaxlawyer

import org.rogach.scallop.ScallopConf
import java.io.File
import com.taj.supertaxlawyer.FileStructure.SizeMain


object Main extends App {
  val testResourcesFolder = s".${File.separator}src${File.separator}test${File.separator}resources${File.separator}"
  val encodedFileFolder = testResourcesFolder + s"encoded_files${File.separator}"

  val file: String = encodedFileFolder + "semicolon.csv"


  val opts = new ScallopConf(List("--columnSize", file, "--splitter", ";")) {
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
    val encoding = opt[String]("encoding", descr = "Print the detected encoding of each file provided.", validate = fileExist)
    val splitter = opt[String]("splitter", descr = "Character used to split a line in columns. Use TAB for tabulation and SPACE for space separators.")
    val columnCount = opt[Int]("columnCount", descr = "[OPTIONAL] Number of columns expected.")

    val output = opt[String]("output", descr = "Path to the file where to save the result.", validate = !new File(_).exists())
    val debug = toggle("debug", descrYes = "Display lots of debug information during the process.", descrNo = "Display minimum during the process (same as not using this argument).", default = Some(false), prefix = "no-")
    val help = opt[Boolean]("help", descr = "Show this message.")
    // val version = opt[Boolean]("version", noshort = true, descr = "Print program version.")
    codependent(columnSize, splitter)

    conflicts(columnSize, List(encoding, help /*, version*/))
    conflicts(encoding, List(columnSize, splitter, columnCount, help /*, version*/))
  }

  val debug = opts.debug.get.getOrElse(false)
  val optionEncoding = opts.encoding.get

  optionEncoding match {
    case Some(path) => println(SizeMain.detectEncoding(path))
    case None =>
  }

  val optionColumnCount = opts.columnCount.get
  val optionSplitter = opts.splitter.get
  val optionOutput = opts.output.get

  val optionColumnSize = opts.columnSize.get
  optionColumnSize match {
    case Some(path) =>
      val splitter: String = optionSplitter match {
        case Some("TAB") => "\t"
        case Some("SPACE") => " "
        case Some(s: String) => s
        case None => throw new IllegalArgumentException("No splitter provided.") // impossible in theory because blocked by ScalaOp
      }

      val file = new File(path)

      val encoding = SizeMain.detectEncoding(path)
      val columnCount = optionColumnCount.getOrElse(SizeMain.columnCount(path, splitter, encoding))
      SizeMain.computeSize(file, splitter, columnCount, encoding, optionOutput, debug)
    case _ =>
  }
}