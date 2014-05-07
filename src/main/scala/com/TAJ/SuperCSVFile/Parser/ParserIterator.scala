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

import com.TAJ.SuperCSVFile.Parser.ParserType._

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
case class ParserIterator(DelimiterChar: Char = ',', QuoteChar: Char = '"', EscapeChar: Char = '\\', IgnoreCharOutsideQuotes: Boolean = false, IgnoreLeadingWhiteSpace: Boolean = true, IteratorOfLines: Iterator[String], BackParseLimit: Option[Int] = Some(1)) extends Iterator[ParserResult] {
  require(BackParseLimit.getOrElse(1) >= 0 && BackParseLimit.getOrElse(1) < 10000, "Limit of the Iterator should be > 0 and < 10 000 for memory reasons")

  private var iteratorParserState: ParserState = ParserState.create(System.getProperty("line.separator"), OpenCSV(DelimiterChar, QuoteChar, EscapeChar, IgnoreCharOutsideQuotes, IgnoreLeadingWhiteSpace), BackParseLimit, () ⇒ IteratorOfLines.hasNext, () ⇒ IteratorOfLines.next())

  override def hasNext: Boolean = iteratorParserState.hasNext

  override def next(): ParserResult = {
    val (newParserState, validation) = CSVParser.parse(iteratorParserState)
    iteratorParserState = newParserState
    validation
  }
}