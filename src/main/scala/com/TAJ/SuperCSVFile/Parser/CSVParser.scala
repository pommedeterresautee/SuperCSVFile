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
import com.TAJ.SuperCSVFile.Parser.ParserType._
import com.TAJ.SuperCSVFile.Parser.ParserType.SuccessLineParser
import com.TAJ.SuperCSVFile.Parser.ParserType.ParserState
import com.TAJ.SuperCSVFile.Parser.ParserType.SuccessParser
import com.TAJ.SuperCSVFile.Parser.ParserType.FailedParser
import com.TAJ.SuperCSVFile.Parser.ParserType.FailedLineParser
import com.TAJ.SuperCSVFile.Parser.ParserType.PendingLineParser

object CSVParser {

  @tailrec
  def parse(parserParameters: FixParserParameters, result: ParserResult, remaining: Option[Int], parserState: ParserState, firstLineOfTheBlock: Option[String]): (ParserState, ParserResult) = {
    val (rawFileLineCounter, newLine, stack: List[String]) = parserState.stack match {
      case Nil          ⇒ (parserState.Counter + 1, parserParameters.getNextLine(), parserState.stack)
      case head :: tail ⇒ (parserState.Counter, head, tail)
    }
    val consumedLinesCounter = rawFileLineCounter - stack.size
    val firstOriginalLineToSendNextRound = firstLineOfTheBlock orElse Some(newLine)
    val (currentResult, newStack) = parserParameters.csvParser.parseLine(newLine, result.PendingParsing, parserParameters.hasOneMoreLine() && remaining.forall(_ > 0) /*add remaining test here because for the parser it is the last line to parse even if there are more in the file.*/ ) match {
      case FailedLineParser(failedMultipleLine) ⇒
        val lineParsed: Seq[String] = failedMultipleLine.head.split(parserParameters.eol, -1).toList
        (FailedParser((result.ParsedLine :+ lineParsed.head) ++ failedMultipleLine.tail, firstOriginalLineToSendNextRound.getOrElse("PARSING ERROR"), result.StartLine, result.StartLine), stack ++ lineParsed.tail)
      case SuccessLineParser(content) ⇒ (SuccessParser(content, result.StartLine, consumedLinesCounter), stack)
      case PendingLineParser(parsedPending, lineParsed) if remaining contains 0 ⇒
        parsedPending.split(parserParameters.eol, -1).toList match {
          case head :: tail ⇒
            (FailedParser(lineParsed :+ head, parsedPending, result.StartLine, result.StartLine), stack ++ tail)
          case Nil ⇒ (SuccessParser(lineParsed, result.StartLine, consumedLinesCounter), stack)
        }
      case PendingLineParser(parsedPending, lineParsed) ⇒ (PendingParser(parsedPending, result.ParsedLine ++ lineParsed, result.StartLine, consumedLinesCounter), stack)
    }
    val newRemaining = remaining.map(_ - 1)
    if (currentResult.isPending && parserParameters.hasOneMoreLine() && newRemaining.forall(_ >= 0)) parse(parserParameters, currentResult, newRemaining, ParserState(rawFileLineCounter, newStack, firstOriginalLineToSendNextRound), firstOriginalLineToSendNextRound)
    else (ParserState(rawFileLineCounter, newStack, None), currentResult)
  }
}
