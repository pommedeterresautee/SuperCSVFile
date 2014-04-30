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

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import com.TAJ.SuperCSVFile.Parser.ParserType._

/**
 * This block of code is inspired from OpenCSV library.
 *
 * Constructs CSVReader with supplied separator and quote char.
 * Allows setting the "strict quotes" and "ignore leading whitespace" flags
 * Attention: don't use the same character for quoting text, for delimiting fields or to escape characters.
 *
 * @param DelimiterChar               the delimiter to use for separating entries
 * @param QuoteChar               the character to use for quoted elements
 * @param EscapeChar                  the character to use for escaping a separator or quote
 * @param IgnoreCharOutsideQuotes            if true, characters outside the quotes are ignored
 * @param IgnoreLeadingWhiteSpace if true, white space in front of a quote in a field is ignored
 */
case class OpenCSV(DelimiterChar: Char = ',', QuoteChar: Char = '"', EscapeChar: Char = '\\', IgnoreCharOutsideQuotes: Boolean = false, IgnoreLeadingWhiteSpace: Boolean = true) {
  require(DelimiterChar != QuoteChar && DelimiterChar != EscapeChar && QuoteChar != EscapeChar, s"Some or all of the parameters of the CSV parser are equal (delimiter [$DelimiterChar], quote [$QuoteChar], escape [$EscapeChar]).")

  val eol = System.getProperty("line.separator")

  //  def parseLine(nextLine: String): Seq[String] = {
  //    parseLine(nextLine, None, MultiLine = false) match {
  //      case FailedParse(result)     ⇒ result
  //      case SuccessParse(result)    ⇒ result
  //      case PendingParse(_, result) ⇒ result
  //    }
  //  }

  /**
   * Parses an incoming String and returns an array of elements.
   *
   * @param currentLine the string to parse
   * @param MultiLine true if we are parsing multiple raw lines for the same CSV line
   * @return the comma-tokenized list of elements, or null if nextLine is null
   */
  def parseLine(currentLine: String, previousPending: Option[String] = None, MultiLine: Boolean = false): ParserState = {
    val tokensOnThisLine: ArrayBuffer[String] = ArrayBuffer()
    val currentToken: StringBuilder = new StringBuilder(128)
    var insideQuotedField: Boolean = false
    var insideField: Boolean = false
    var position: Int = 0
    var previousCharWasQuoteChar = false

      def isThereMoreChar: Boolean = currentLine.length > (position + 1)

      def isTherePreviousChar: Boolean = position > 2

      def isNextToDelimiterChar: Boolean = isTherePreviousChar && currentLine(position - 1) != DelimiterChar && isThereMoreChar && currentLine(position + 1) != DelimiterChar

      def isThereMoreCharOrInQuoteOrInField: Boolean = (insideQuotedField || insideField) && isThereMoreChar

      def isNextCharacter(char: Char*): Boolean = char.exists(currentLine(position + 1) ==)

    previousPending match {
      case Some(pendingToken) if MultiLine && currentLine == null ⇒ return FailedParse(Seq[String](pendingToken))
      case Some(pendingToken) ⇒
        // get the pending token from the previous line parsing process
        currentToken ++= pendingToken
        insideQuotedField = true
      // current line is empty, get the pending token (may be end of file?)
      case _ ⇒
    }

    while (position < currentLine.length) {
      val char = currentLine(position)
      char match {
        case QuoteChar if !previousCharWasQuoteChar && isThereMoreCharOrInQuoteOrInField && isNextCharacter(QuoteChar) ⇒ // the next char is a quote, so it is a double quote
          previousCharWasQuoteChar = true
        case QuoteChar if !previousCharWasQuoteChar ⇒ // there is only ONE quote
          insideQuotedField = !insideQuotedField
          if (!IgnoreCharOutsideQuotes && isNextToDelimiterChar && IgnoreLeadingWhiteSpace && currentToken.toArray.forall(Character.isWhitespace)) { // not opening or closing quoted field
            currentToken clear () // remove current built token if only made of spaces
          }
          insideField = !insideField // open and close the state in quote
        case DelimiterChar if !insideQuotedField ⇒
          tokensOnThisLine += currentToken.toString()
          currentToken clear ()
          insideField = false
        case EscapeChar ⇒ // drop escape char
        case _ if !IgnoreCharOutsideQuotes || insideQuotedField ⇒
          previousCharWasQuoteChar = false
          currentToken += char
          insideField = true
      }
      position += 1
    }

    insideQuotedField match {
      case true if MultiLine ⇒ // in quote and the line is not finished
        currentToken ++= eol
        PendingParse(currentToken.toString(), tokensOnThisLine)
      case true ⇒ // in quote and there is no more content to add
        tokensOnThisLine += currentToken.toString()
        FailedParse(tokensOnThisLine)
      case false ⇒ // not in quoted field
        tokensOnThisLine += currentToken.toString()
        SuccessParse(tokensOnThisLine)
    }
  }
}