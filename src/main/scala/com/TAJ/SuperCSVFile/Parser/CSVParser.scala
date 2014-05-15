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
  private def parseOneLine(originalParserState: ParserState, rawFileLineCounter: Int, newLine: String, stack: Seq[String], hasNext: Boolean): (ParserState, ParserResult) = {
    val consumedLinesCounter = rawFileLineCounter - stack.size
    val firstOriginalLineToSendNextRound = originalParserState.firstLineOfTheBlock orElse Some(newLine)
    val (currentResult, newStack, pending, parsedLine) = originalParserState.csvParser.parseLine(newLine, originalParserState.PendingParsing, hasNext && originalParserState.remaining.forall(_ > 0) /*add remaining test here because for the parser it is the last line to parse even if there are more in the file.*/ ) match {
      case FailedLineParser(failedMultipleLine) ⇒
        val lineParsed: Seq[String] = failedMultipleLine.head.split(originalParserState.eol, -1).toList
        (FailedParser((originalParserState.ParsedLine :+ lineParsed.head) ++ failedMultipleLine.tail, firstOriginalLineToSendNextRound.getOrElse("PARSING ERROR"), originalParserState.StartLine), stack ++ lineParsed.tail, None, List.empty)
      case SuccessLineParser(content) ⇒ (SuccessParser(content, originalParserState.StartLine, consumedLinesCounter), stack, None, List.empty)
      case PendingLineParser(parsedPending, lineParsed) if originalParserState.remaining contains 0 ⇒
        parsedPending.split(originalParserState.eol, -1).toList match {
          case head :: tail ⇒
            (FailedParser(lineParsed :+ head, parsedPending, originalParserState.StartLine), stack ++ tail, None, List.empty)
          case Nil ⇒ (SuccessParser(lineParsed, originalParserState.StartLine, consumedLinesCounter), stack, None, List.empty)
        }
      case PendingLineParser(parsedPending, lineParsed) ⇒ (PendingParser, stack, Some(parsedPending), originalParserState.ParsedLine ++ lineParsed)
    }
    val newRemaining = originalParserState.remaining.map(_ - 1)
    val newState = ParserState.updateState(rawFileLineCounter, newStack, firstOriginalLineToSendNextRound, pending, parsedLine, newRemaining, originalParserState.StartLine)(originalParserState)
    (newState, currentResult)
  }

  @tailrec
  def parse(originalParserState: ParserState, hasSourceOneMoreLine: () ⇒ Boolean, getSourceNextLine: () ⇒ String): (ParserState, ParserResult) = {
    val (rawFileLineCounter, newLine, stack) = originalParserState.stack match {
      case Nil          ⇒ (originalParserState.counter + 1, getSourceNextLine(), originalParserState.stack)
      case head :: tail ⇒ (originalParserState.counter, head, tail)
    }
    val (newState, currentResult) = parseOneLine(originalParserState, rawFileLineCounter, newLine, stack, hasSourceOneMoreLine())
    if (newState.PendingParsing.isDefined && hasSourceOneMoreLine() && newState.remaining.forall(_ >= 0)) parse(newState, hasSourceOneMoreLine, getSourceNextLine)
    else {
      val finalState = ParserState.newStateForNextIteration(newState)
      (finalState, currentResult)
    }
  }
}