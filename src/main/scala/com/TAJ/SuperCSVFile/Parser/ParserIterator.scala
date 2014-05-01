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

import scalaz._
import Scalaz._
import scala.collection.mutable
import com.TAJ.SuperCSVFile.Parser.ParserType._
import scala.annotation.tailrec

/**
 * Provide an Iterator of String and get an Iterator of parsed CSV lines.
 *
 * @param DelimiterChar Delimit each field
 * @param QuoteChar Avoid interpetration of the text inside a pair of this character
 * @param EscapeChar Used to escape some characters
 * @param IgnoreCharOutsideQuotes as indicated by the name
 * @param IgnoreLeadingWhiteSpace as indicated by the name
 * @param IterartorOfLines Iterator of string to parse
 * @param BackParseLimit When in a quoted field, defines the number of line to look forward for the second quote.
 */
case class ParserIterator(DelimiterChar: Char = ',', QuoteChar: Char = '"', EscapeChar: Char = '\\', IgnoreCharOutsideQuotes: Boolean = false, IgnoreLeadingWhiteSpace: Boolean = true, IterartorOfLines: Iterator[String], BackParseLimit: Option[Int] = Some(1)) extends Iterator[Seq[String]] {
  require(BackParseLimit.getOrElse(1) >= 0 && BackParseLimit.getOrElse(1) < 10000, "Limit of the Iterator should be > 0 and < 10 000 for memory reasons")

  private val eol = System.getProperty("line.separator")

  private val parser = OpenCSV(DelimiterChar, QuoteChar, EscapeChar, IgnoreCharOutsideQuotes, IgnoreLeadingWhiteSpace)

  var LineStack: mutable.Stack[String] = mutable.Stack()

  override def hasNext: Boolean = IterartorOfLines.hasNext || LineStack.size > 0

  override def next(): Seq[String] = {
    var remaining = BackParseLimit.getOrElse(1)
    var CurrentPending: Option[String] = None

      def getNextLine = if (LineStack.size > 0) LineStack.pop() else IterartorOfLines.next()

      @tailrec
      def parse(result: Seq[String]): Seq[String] = {
        val currentresult = parser.parseLine(getNextLine, CurrentPending, hasNext && remaining > 0 /*add remaining test here because for the parser it is the last line to parse even if there are more in the file.*/ ) match {
          case FailedParse(failedLine) ⇒
            CurrentPending = None
            val lineParsed: Seq[String] = failedLine.head.split(eol, -1).toList
            LineStack ++= lineParsed.tail
            result ++ Seq(lineParsed.head) ++ failedLine.tail
          case SuccessParse(lineParsed) ⇒
            CurrentPending = None
            lineParsed
          case PendingParse(parsedPending, lineParsed) if remaining == 0 ⇒
            parsedPending.split(eol, -1).toList match {
              case head :: tail ⇒
                LineStack ++= tail
                lineParsed :+ head
              case Nil ⇒ lineParsed
            }
          case PendingParse(parsedPending, lineParsed) ⇒
            CurrentPending = parsedPending.some
            result ++ lineParsed
        }
        if (BackParseLimit.isDefined) remaining -= 1

        if (CurrentPending.isDefined && hasNext && remaining >= 0) {
          parse(currentresult)
        }
        else currentresult
      }

    parse(Seq())
  }
}