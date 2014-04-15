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

package com.TAJ.SuperCSVFile.Parser

import scala.annotation.tailrec

/**
 * Parse CSV file:
 * - replace double quotes by simple one
 * - don't interpret delimiter inside quoted field
 */
object CSVParserRecusrive extends App {

  def parseLines(linesToProcess: Seq[String], delimiter: Char, quote: Char): Seq[Seq[String]] = {
    parseLines(linesToProcess, Seq(), delimiter, quote)
  }

  /**
   * Parse one line according to the RFC 4180.
   * @param line String to process
   * @param delimiter Character to use to delimit each field.
   * @param quote Character to use for quoted fields as described by the CSV specification.
   * @return the sequence of parsed fields.
   */
  def parsseLine(line: String, delimiter: Char, quote: Char): Seq[String] = {
    OpenCSV(delimiter = delimiter).parseLine(line)
    line.split(delimiter)
    //parseLine(line, Seq(), Seq(), delimiter, quote)
  }

  /**
   * Parse a group of lines.
   * @param linesToProcess the original String to process
   * @param linesProcessed result storage
   * @param delimiter character used to separate fields
   * @param quote character used to stop interpretation
   * @return a Sequence of Sequence of fields content
   */
  @tailrec
  private def parseLines(linesToProcess: Seq[String], linesProcessed: Seq[Seq[String]], delimiter: Char, quote: Char): Seq[Seq[String]] = {
    linesToProcess match {
      case Nil ⇒ linesProcessed
      case head :: tail ⇒
        val line = parsseLine(head, delimiter, quote)
        parseLines(tail, linesProcessed :+ line, delimiter, quote)
    }
  }

  /**
   * Parse one line to extract each field.
   */
  @tailrec
  private def parseLine(toProcess: Seq[Char], currentWord: Seq[Char], parsedLine: Seq[String], delimiter: Char, quote: Char): Seq[String] = {
    toProcess match {
      case Nil ⇒ parsedLine :+ currentWord.mkString
      case Seq(delimiterChar, quoteChar, next, after, tail @ _*) if (delimiterChar == delimiter && quoteChar == quote && next != quote) ||
        (delimiterChar == delimiter && quoteChar == quote && next == quote && after == quote) ⇒
        val (quotedField, stillToProcess) = parseQuote(next +: after +: tail, Seq(), delimiter, quote)
        (parsedLine :+ currentWord.mkString, toProcess)
        parseLine(stillToProcess, Seq(), parsedLine :+ currentWord.mkString :+ quotedField.mkString, delimiter, quote)
      case Seq(head, next, tail @ _*) if head == quote && next == quote ⇒
        parseLine(tail, currentWord :+ head, parsedLine, delimiter, quote)
      case Seq(head, tail @ _*) if head == delimiter ⇒
        parseLine(tail, Seq(), parsedLine :+ currentWord.mkString, delimiter, quote)
      case Seq(head, tail @ _*) ⇒
        parseLine(tail, currentWord :+ head, parsedLine, delimiter, quote)
    }
  }

  /**
   * Parse a quote (to use inside a quoted field only).
   */
  @tailrec
  private def parseQuote(toProcess: Seq[Char], quotePart: Seq[Char], delimiter: Char, quote: Char): (String, String) = {
    toProcess match {
      case Nil ⇒ (quotePart.mkString, "")
      case Seq(head, next, tail @ _*) if head == quote && next == quote ⇒ // double quote -> remove one quote
        parseQuote(tail, quotePart :+ head, delimiter, quote)
      case Seq(head, next, stillToProcess @ _*) if head == quote && next == delimiter ⇒ //end quote and not last element
        (quotePart.mkString, stillToProcess.mkString)
      case Seq(head) if head == quote ⇒ //end quote last element
        (quotePart.mkString, "")
      case Seq(head, tail @ _*) ⇒ // inside the quote
        parseQuote(tail, quotePart :+ head, delimiter, quote)
    }
  }

  val chars = Seq("line 1 - field 1,line 1 - field 2,line 1 - field 3,line 1 - field 4,\"block1,block2\"", "line 2 - field 1,hihi,koko,kiki")

  println(parseLines(chars, ',', '"')(0).mkString("\n"))

}
