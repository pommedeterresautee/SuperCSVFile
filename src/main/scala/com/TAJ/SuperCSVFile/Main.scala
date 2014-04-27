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

package com.TAJ.SuperCSVFile

import java.io.File
import com.TAJ.SuperCSVFile.CommandLine.ExecuteCommandLine
import com.TAJ.SuperCSVFile.Parser.{ ParserIterator, OpenCSV }

/**
 * Main entry in the program.
 * Class to launch at start.
 */
object Main extends App {
  val testResourcesFolder = s".${File.separator}src${File.separator}test${File.separator}resources${File.separator}"
  val encodedFileFolder = testResourcesFolder + s"encoded_files${File.separator}"

  val file: String = encodedFileFolder + "airports.csv"

  val linux = "/home/geantvert/SCC324_319472775FEC20121231.txt"
  val windowsFileUTF8 = "C:\\Users\\MBenesty\\Private\\GIT\\SuperCSVFile\\FEC_EXAMPLE\\FEC_UTF8_TAB.txt"
  val file2UTF8 = encodedFileFolder + "utf8_file_bis.txt"
  val argUTF8 = Array("--columnSize", "--inputFile", windowsFileUTF8, "--encoding", "ISO-8859-1", "--titlesExcluded", "--firstLine", "1")
  val arg = Array("--columnSize", file)
  val argExtract = Array("--inputFile", windowsFileUTF8, "--linesCount", "--columnSize")
  val help = Array("--help")

  //  ExecuteCommandLine(args)

  val toParse = """test;test2
                  |"seconde ligne
                  |troisieme ligne
                  |quatrieme ligne;test3
    |encore;deux;etTrois
    |fmklsgnal;fnghka"
    |
    |ckdnsklgfasg;fnsdkjagf""".stripMargin.split('\n').toIterator

  val parser = OpenCSV(DelimiterChar = ';')
  val par = ParserIterator(parser, toParse)
  par.zipWithIndex.foreach { case (line, index) ⇒ println(index + 1 + ": [" + line.mkString("|") + "]") }

}