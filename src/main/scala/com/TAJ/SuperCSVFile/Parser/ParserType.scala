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

object ParserType {
  type StringStack = mutable.Stack[String]

  case class ParserState(Counter: Int, stack: Seq[String], firstLineOfTheBlock: Option[String], PendingParsing: Option[String], ParsedLine: Seq[String], remaining: Option[Int], StartLine: Int)

  case class FixParserParameters(eol: String, csvParser: OpenCSV, hasOneMoreLine: () ⇒ Boolean, getNextLine: () ⇒ String, BackParseLimit: Option[Int]) {
    val initialState: ParserState = ParserState(-1, Seq(), None, None, Seq(), BackParseLimit, 0)
  }

  sealed trait LineParserValidation {
    val ParsedLine: Seq[String]
    val isSuccess: Boolean = false
    val isFail: Boolean = false
    val isPending: Boolean = false
    val PendingParsing: Option[String] = None
  }

  sealed trait ParserResult extends LineParserValidation {
    val StartLine: Int
    val EndLine: Int
    val RawString: Option[String] = None
  }

  case class SuccessLineParser(ParsedLine: Seq[String]) extends LineParserValidation {
    override val isSuccess = true
  }

  case class FailedLineParser(ParsedLine: Seq[String]) extends LineParserValidation {
    override val isFail = true
  }

  case class PendingLineParser(CurrentToken: String, ParsedLine: Seq[String]) extends LineParserValidation {
    override val isPending = true
    override val PendingParsing = Some(CurrentToken)
  }

  case class SuccessParser(ParsedLine: Seq[String], StartLine: Int, EndLine: Int) extends ParserResult {
    override val isSuccess = true
  }

  case class FailedParser(ParsedLine: Seq[String], OriginalString: String, StartLine: Int) extends ParserResult {
    override val isFail = true
    override val RawString = Some(OriginalString)
    override val EndLine: Int = StartLine
  }

  object PendingParser extends ParserResult {
    override val isPending = true
    override val ParsedLine: Seq[String] = List.empty
    override val StartLine: Int = 0
    override val EndLine: Int = 0
  }
}
