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

import scala.collection.mutable
import com.TAJ.SuperCSVFile.Parser.ParserType._
import scala.annotation.tailrec

/**
 * Provide an Iterator of String and get an Iterator of parsed CSV lines.
 *
 * @param DelimiterChar Delimit each field
 * @param QuoteChar Avoid interpretation of the text inside a pair of this character
 * @param EscapeChar Used to escape some characters
 * @param IgnoreCharOutsideQuotes as indicated by the name
 * @param IgnoreLeadingWhiteSpace as indicated by the name
 * @param IteratorOfLines Iterator of string to parse
 * @param BackParseLimit When in a quoted field, defines the number of line to look forward for the second quote.
 */
case class ParserIterator(DelimiterChar: Char = ',', QuoteChar: Char = '"', EscapeChar: Char = '\\', IgnoreCharOutsideQuotes: Boolean = false, IgnoreLeadingWhiteSpace: Boolean = true, IteratorOfLines: Iterator[String], BackParseLimit: Option[Int] = Some(1)) extends Iterator[ParserValidation] {
  require(BackParseLimit.getOrElse(1) >= 0 && BackParseLimit.getOrElse(1) < 10000, "Limit of the Iterator should be > 0 and < 10 000 for memory reasons")

  private val eol = System.getProperty("line.separator")

  private val parser = OpenCSV(DelimiterChar, QuoteChar, EscapeChar, IgnoreCharOutsideQuotes, IgnoreLeadingWhiteSpace)

  var LineStack: StringStack = mutable.Stack()
  var lineCounter = -1

  private def getLineNumber = lineCounter - LineStack.size

  override def hasNext: Boolean = IteratorOfLines.hasNext || !LineStack.isEmpty

  @tailrec
  private def parse(result: ParserValidation, remaining: Option[Int], firstLineOfTheBlock: Option[String]): ParserValidation = {
    val nextLine = if (!LineStack.isEmpty) LineStack.pop() else {
      lineCounter += 1
      IteratorOfLines.next()
    }
    val firstOriginalLineToSendNextRound = firstLineOfTheBlock orElse Some(nextLine)

    val currentResult: ParserValidation = parser.parseLine(nextLine, result.PendingParsing, hasNext && remaining.forall(_ > 0) /*add remaining test here because for the parser it is the last line to parse even if there are more in the file.*/ ) match {
      case FailedLineParser(failedMultipleLine) ⇒
        val lineParsed: Seq[String] = failedMultipleLine.head.split(eol, -1).toList
        LineStack ++= lineParsed.tail
        FailedParser((result.ParsedLine :+ lineParsed.head) ++ failedMultipleLine.tail, firstOriginalLineToSendNextRound.get, result.StartLine, result.StartLine)
      case SuccessLineParser(content) ⇒ SuccessParser(content, result.StartLine, getLineNumber)
      case PendingLineParser(parsedPending, lineParsed) if remaining contains 0 ⇒
        parsedPending.split(eol, -1).toList match {
          case head :: tail ⇒
            LineStack ++= tail
            FailedParser(lineParsed :+ head, parsedPending, result.StartLine, result.StartLine)
          case Nil ⇒ SuccessParser(lineParsed, result.StartLine, getLineNumber)
        }
      case PendingLineParser(parsedPending, lineParsed) ⇒ PendingParser(parsedPending, result.ParsedLine ++ lineParsed, result.StartLine, getLineNumber)
    }
    val newRemaining = remaining.map(_ - 1)

    if (currentResult.isPending && hasNext && newRemaining.forall(_ >= 0)) parse(currentResult, newRemaining, firstOriginalLineToSendNextRound)
    else currentResult
  }

  override def next(): ParserValidation = parse(SuccessParser(Seq(), getLineNumber + 1, getLineNumber + 1), BackParseLimit, None)
}