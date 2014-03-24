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

package com.taj.supertaxlawyer.FileStructure

import scala.io.Source
import akka.actor._
import com.ibm.icu.text.CharsetDetector
import java.io.{BufferedInputStream, File, FileInputStream}
import com.taj.supertaxlawyer.ActorMessages.Start
import com.taj.supertaxlawyer.{ActorContainer, Distributor}
import java.nio.charset.Charset

/**
 * Operation related to the count of columns in a text file.
 */
object FileSizeTools {

  /**
   * Compute the size of each column in the text file.
   * @param path path to the text file.
   * @param splitter Char or String used to limit the columns.
   * @param expectedColumnQuantity number of columns expected per line. Any line with a different number of column won't be tested.
   * @param encoding Encoding of the text file.
   * @return A list of column sizes.
   */
  def computeSize(path: File, splitter: String, expectedColumnQuantity: Int, encoding: String, output: Option[String], titles: Option[List[String]]) {
    implicit val system: ActorSystem = ActorSystem("ActorSystemComputation")

    val listOfWorkers: List[ActorContainer] = List(
      SizeActor(output, expectedColumnQuantity, splitter, titles),
      LineCounterActor(output) /*,
      ExtractEntry(expectedColumnQuantity, splitter)*/
    )
    val dropLines = if (titles.isDefined) 1 else 0
    val distributor = Distributor(path, splitter, expectedColumnQuantity, encoding, listOfWorkers, dropLines)
    distributor ! Start()
  }

  def findDelimiter(path: String, encoding: String):(String, Int) = {
    val buffer = Source.fromFile(path, encoding)
    buffer
      .getLines()
      .take(1000)
      .map(line => line.groupBy(_.toChar).mapValues(c => c.size))
      .map(map => ???)
      ???
  }

  /**
   * Count the number of columns in a text file.
   * @param path path to the file to study.
   * @param splitter String used to limit the columns.
   * @param encoding encoding of the file
   * @return the number of columns in the text file.
   */
  def columnCount(path: String, splitter: String, encoding: String): Int = {
    val splitterWithEscapedChar = s"\\Q$splitter\\E"
    val buffer = Source.fromFile(path, encoding)
    val (numberOfColumns, _) = buffer
      .getLines()
      .take(1000)
      .toList
      .groupBy(line => line.split(splitterWithEscapedChar).size)
      .map {
      case (numberOfTimes, listOfColumns) => (numberOfTimes, listOfColumns.size)
    }
      .maxBy {
      case (numberOfTimes, numberOfColumnsPerLine) => numberOfTimes
    }
    buffer.close()
    numberOfColumns
  }

  /**
   * Detects the encoding of a text file based on Heuristic analyze.
   * @param path path to the file to analyze.
   * @return the name of the encoding as a String.
   */
  def detectEncoding(path: String): String = {
    val detector = new CharsetDetector()
    val is = new BufferedInputStream(new FileInputStream(path))
    try detector.setText(is)
    finally is.close()
    val matcher = detector.detect()
    matcher.getName
  }
}