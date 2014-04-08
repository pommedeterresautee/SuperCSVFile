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

package com.TAJ.SuperCSVFile.FileStructure

import scala.io.Source
import com.ibm.icu.text.CharsetDetector
import java.io.{ BufferedInputStream, FileInputStream }
import com.typesafe.scalalogging.slf4j.Logging
import java.util.regex.Pattern

/**
 * Operation related to the count of columns in a text file.
 */
object FileTools extends Logging {

  /**
   * <p>Find the delimiter and the number of columns based on the first 1000 lines.</p>
   * <p>In case of several characters equally present, choose the one with the highest frequency per line.</p><br>
   * <p><b>WARNING</b>: May fail on small text file (less than 50 lines).</p><br>
   * @param path to the file to analyze.
   * @param encoding of the text file.
   * @return a Tuple with the delimiter and the number of columns found.
   */
  def findColumnDelimiter(path: String, encoding: String): (String, Int) = {
    val tupleList =
      Source.fromFile(path, encoding)
        .getLines()
        .take(1000)
        .map(line ⇒ line.filter(!_.isLetterOrDigit)) // remove letters and digits
        .map(filteredLine ⇒ filteredLine.groupBy(_.toChar).mapValues(char ⇒ char.size)) // return the frequency of each char per line.
        .flatMap(map ⇒ map.toList) // transform each Map in List of Tuple and then flat the Iterator of List of Tuple.
        .toList
        .groupBy(tuple ⇒ tuple) // enumerate each Tuple (Char, Quantity) in the big list and group them.
        .map { case (tuple, list) ⇒ (tuple, list.length) }
        .toList

    val (_, max) = tupleList
      .maxBy {
        case (tuple, tupleQuantity) ⇒ tupleQuantity
      } // search for the most frequent Tuple.

    val ((delimiter, frequency), _) = tupleList
      .filter { case (tuple, tupleFrequency) ⇒ tupleFrequency == max } // keep only the most frequent Tuple in the entire file.
      .maxBy(_._1._2) // choose the Character which has the highest frequency per line.

    val delimiterEscaped = Pattern.quote(delimiter.toString) // escape special characters (in particular pipe (|))

    logger.debug(s"*** The character the most recurrent is [$delimiter] and its frequency per line is equal to $frequency. It appears in $max lines. ***")

    (delimiterEscaped, frequency + 1) // +1 because there is always one column more than quantity of delimiters.
  }

  /**
   * Count the number of columns in a text file.
   * @param path path to the file to study.
   * @param splitter String used to limit the columns.
   * @param encoding encoding of the file
   * @return the number of columns in the text file.
   */
  def columnCount(path: String, splitter: String, encoding: String): Int = {
    val buffer = Source.fromFile(path, encoding)
    val (numberOfColumns, _) = buffer
      .getLines()
      .take(1000)
      .toList
      .groupBy(line ⇒ line.split(splitter).size)
      .map {
        case (numberOfTimes, listOfColumns) ⇒ (numberOfTimes, listOfColumns.size)
      }
      .maxBy {
        case (numberOfTimes, numberOfColumnsPerLine) ⇒ numberOfTimes
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