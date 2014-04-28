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
import scalaz._
import Scalaz._

sealed trait CSVParser
case class Pending(result: Validation[Seq[String], Seq[String]], pendingLine: String) extends CSVParser
case class Parsed(result: Validation[Seq[String], Seq[String]]) extends CSVParser

/**
 * This block of code is inspired from OpenCSV library.
 *
 * Constructs CSVReader with supplied separator and quote char.
 * Allows setting the "strict quotes" and "ignore leading whitespace" flags
 *
 * @param DelimiterChar               the delimiter to use for separating entries
 * @param QuoteChar               the character to use for quoted elements
 * @param EscapeChar                  the character to use for escaping a separator or quote
 * @param ignoreCharOutsideQuotes            if true, characters outside the quotes are ignored
 * @param ignoreLeadingWhiteSpace if true, white space in front of a quote in a field is ignored
 */
case class OpenCSV(private val DelimiterChar: Char = ',', private val QuoteChar: Char = '"', private val EscapeChar: Char = '\\', private val ignoreCharOutsideQuotes: Boolean = false, private val ignoreLeadingWhiteSpace: Boolean = true) {
  require(DelimiterChar != QuoteChar && DelimiterChar != EscapeChar && QuoteChar != EscapeChar, s"Some or all of the parameters of the CSV parser are equal (delimiter [$DelimiterChar], quote [$QuoteChar], escape [$EscapeChar]).")

  type ParserResult[A] = Validation[Seq[A], Seq[A]]

  type StateParsing[A] = (Option[A], ParserResult[A])

  val eol = System.getProperty("line.separator")

  def parseLine(nextLine: String): ParserResult[String] = {
    val (_, result) = parseLine(nextLine, None, MultiLine = false)
    result
  }

  /**
   * Parses an incoming String and returns an array of elements.
   *
   * @param currentLine the string to parse
   * @param MultiLine true if we are parsing multiple raw lines for the same CSV line
   * @return the comma-tokenized list of elements, or null if nextLine is null
   */
  def parseLine(currentLine: String, previousPending: Option[String], MultiLine: Boolean): StateParsing[String] = {
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
      case Some(pendingToken) if MultiLine && currentLine == null ⇒ return (None, Seq[String](pendingToken).failure)
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
          if (!ignoreCharOutsideQuotes && isNextToDelimiterChar) { // not opening or closing quoted field
            if (ignoreLeadingWhiteSpace && currentToken.toArray.forall(Character.isWhitespace))
              currentToken clear () // remove current built token if only made of spaces
            else currentToken += char // add the text to the current token
          }
          insideField = !insideField // open and close the state in quote
        case DelimiterChar if !insideQuotedField ⇒
          tokensOnThisLine += currentToken.toString()
          currentToken clear ()
          insideField = false
        case EscapeChar ⇒ // drop escape char
        case _ if !ignoreCharOutsideQuotes || insideQuotedField ⇒
          previousCharWasQuoteChar = false
          currentToken += char
          insideField = true
      }
      position += 1
    }

    insideQuotedField match {
      case true if MultiLine ⇒ // in quote and the line is not finished
        currentToken ++= eol
        (currentToken.toString().some, tokensOnThisLine.success)
      case true ⇒ // in quote and there is no more content to add
        tokensOnThisLine += currentToken.toString()
        (None, tokensOnThisLine.failure)
      case false ⇒ // not in quoted field
        tokensOnThisLine += currentToken.toString()
        (None, tokensOnThisLine.success)
    }
  }
}