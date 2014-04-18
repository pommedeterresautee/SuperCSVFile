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

/**
 * This block of code is inspired from OpenCSV library.
 *
 * Constructs CSVReader with supplied separator and quote char.
 * Allows setting the "strict quotes" and "ignore leading whitespace" flags
 *
 * @param delimiterChar               the delimiter to use for separating entries
 * @param quoteChar               the character to use for quoted elements
 * @param escapeChar                  the character to use for escaping a separator or quote
 * @param ignoreCharOutsideQuotes            if true, characters outside the quotes are ignored
 * @param ignoreLeadingWhiteSpace if true, white space in front of a quote in a field is ignored
 */
case class OpenCSV(delimiterChar: Char = ',', quoteChar: Char = '"', escapeChar: Char = '\\', ignoreCharOutsideQuotes: Boolean = false, ignoreLeadingWhiteSpace: Boolean = true) {
  require(delimiterChar != quoteChar && delimiterChar != escapeChar && quoteChar != escapeChar, s"Some or all of the parameters of the CSV parser are equal (delimiter [$delimiterChar], quote [$quoteChar], escape [$escapeChar]).")

  private var pending: Option[String] = None

  def parseLineMulti(nextLine: String): Validation[Seq[String], Seq[String]] = parseLine(nextLine, multiLine = true)

  def parseLine(nextLine: String): Validation[Seq[String], Seq[String]] = parseLine(nextLine, multiLine = false)

  /**
   * Parses an incoming String and returns an array of elements.
   *
   * @param nextLine the string to parse
   * @param multiLine true if we are parsing multiple raw lines for the same CSV line
   * @return the comma-tokenized list of elements, or null if nextLine is null
   * @throws IOException if bad things happen during the read
   */
  private def parseLine(nextLine: String, multiLine: Boolean): Validation[Seq[String], Seq[String]] = {
    val tokensOnThisLine: ArrayBuffer[String] = ArrayBuffer()
    val currentToken: StringBuilder = new StringBuilder(128)
    var insideQuotedField: Boolean = false
    var insideField: Boolean = false
    var position: Int = 0

      def isThereMoreChar: Boolean = nextLine.length > (position + 1)

      def isTherePreviousChar: Boolean = position > 2

      def isPreviousAddedCharAQuote: Boolean = currentToken.length > 0 && currentToken(currentToken.length - 2) == quoteChar

      def isThereMoreCharOrInQuoteOrInField: Boolean = (insideQuotedField || insideField) && isThereMoreChar

      def isNextCharacter(char: Char*): Boolean = char.exists(nextLine(position + 1) ==)

    if (!multiLine && pending.isDefined) {
      pending = None
    }

    if (nextLine == null) { // current line is empty, get the pending token (may be end of file?)
      pending match {
        case Some(pendingToken) ⇒ return Success(Seq[String](pendingToken))
        case None               ⇒ return null
      }
    }

    pending match { // get the pending token from the previous line parsing process
      case Some(pendingToken) ⇒
        currentToken.append(pendingToken)
        pending = None
        insideQuotedField = true
      case _ ⇒
    }

    while (position < nextLine.length) {
      val char: Char = nextLine(position)

      char match {
        case _ if char == escapeChar ⇒ // can be filtered in a Monad
        //          if (isThereMoreCharOrInQuoteOrInField && isNextCharacter(quoteChar, escapeChar)) {
        //            currentToken.append(nextLine(position + 1))
        //            position += 1 // Jump one char
        //          }
        case _ if char == quoteChar && isThereMoreCharOrInQuoteOrInField && isNextCharacter(quoteChar) ⇒ // the next char is a quote, so it is a double quote
          currentToken.append(char) // add the quote char directly to the
          position += 1 // Jump one char
        case _ if char == quoteChar ⇒ // there is only ONE quote
          insideQuotedField = !insideQuotedField
          if (!ignoreCharOutsideQuotes && isTherePreviousChar && nextLine(position - 1) != delimiterChar && isThereMoreChar && nextLine(position + 1) != delimiterChar) { // not opening or closing quoted field
            if (ignoreLeadingWhiteSpace && currentToken.toArray.forall(Character.isWhitespace)) currentToken.clear()
            else currentToken.append(char) // add the text to the current token
          }
          insideField = !insideField // open and close the state in quote
        case _ if char == delimiterChar && !insideQuotedField ⇒
          tokensOnThisLine += currentToken.toString()
          currentToken.setLength(0)
          insideField = false
        case _ ⇒
          if (!ignoreCharOutsideQuotes || insideQuotedField) {
            currentToken.append(char)
            insideField = true
          }
      }
      position += 1
    }

    if (insideQuotedField) {
      if (multiLine) {
        currentToken.append("\n")
        pending = Some(currentToken.toString())
        currentToken.clear()
      }
      else { // in quote and the line is finished
        tokensOnThisLine += currentToken.toString()
        return Failure(tokensOnThisLine)
      }
    }

    tokensOnThisLine += currentToken.toString()
    Success(tokensOnThisLine)
  }
}