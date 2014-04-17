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

import java.io.IOException
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

/**
 * This block of code is inspired from OpenCSV library.
 *
 * Constructs CSVReader with supplied separator and quote char.
 * Allows setting the "strict quotes" and "ignore leading whitespace" flags
 *
 * @param delimiterChar               the delimiter to use for separating entries
 * @param quoteChar               the character to use for quoted elements
 * @param escapeChar                  the character to use for escaping a separator or quote
 * @param strictQuotes            if true, characters outside the quotes are ignored
 * @param ignoreLeadingWhiteSpace if true, white space in front of a quote in a field is ignored
 */
case class OpenCSV(delimiterChar: Char = ',', quoteChar: Char = '"', escapeChar: Char = '\\', strictQuotes: Boolean = false, ignoreLeadingWhiteSpace: Boolean = true) {
  require(delimiterChar != quoteChar && delimiterChar != escapeChar && quoteChar != escapeChar, s"Some or all of the parameters of the CSV parser are equal (delimiter [$delimiterChar], quote [$quoteChar], escape [$escapeChar]).")

  private var pending: Option[String] = None
  private var inField: Boolean = false

  def parseLineMulti(nextLine: String): Seq[String] = parseLine(nextLine, multiLine = true)

  def parseLine(nextLine: String): Seq[String] = parseLine(nextLine, multiLine = false)

  /**
   * Parses an incoming String and returns an array of elements.
   *
   * @param nextLine the string to parse
   * @param multiLine true if we are parsing multiple raw lines for the same CSV line
   * @return the comma-tokenized list of elements, or null if nextLine is null
   * @throws IOException if bad things happen during the read
   */
  private def parseLine(nextLine: String, multiLine: Boolean): Seq[String] = {
    val tokensOnThisLine: ArrayBuffer[String] = ArrayBuffer()
    val currentToken: StringBuilder = new StringBuilder(128)
    var inQuotes: Boolean = false
    var position: Int = 0

      def isThereMoreChar: Boolean = nextLine.length > (position + 1)

      def isThereMoreCharOrInQuoteOrInField: Boolean = (inQuotes || inField) && isThereMoreChar

      def isNextCharacter(char: Char*): Boolean = char.exists(nextLine(position + 1) ==)

    if (!multiLine && pending.isDefined) {
      pending = None
    }

    if (nextLine == null) {
      pending match {
        case Some(pendingToken) ⇒ return Seq[String](pendingToken)
        case None               ⇒ return null
      }
    }

    pending match {
      case Some(pendingToken) ⇒
        currentToken.append(pendingToken)
        pending = None
        inQuotes = true
      case _ ⇒
    }

    while (position < nextLine.length) {
      val char: Char = nextLine(position)

      char match {
        case _ if char == escapeChar ⇒
          if (isThereMoreCharOrInQuoteOrInField && isNextCharacter(quoteChar, escapeChar)) {
            currentToken.append(nextLine(position + 1))
            position += 1 // Jump one char
          }
        case _ if char == quoteChar ⇒
          if (isThereMoreCharOrInQuoteOrInField && isNextCharacter(quoteChar)) {
            currentToken.append(nextLine(position + 1))
            position += 1 // Jump one char
          }
          else {
            if (!strictQuotes) {
              if (position > 2 && nextLine(position - 1) != delimiterChar && isThereMoreChar && nextLine(position + 1) != delimiterChar) {
                if (ignoreLeadingWhiteSpace && currentToken.toArray.forall(Character.isWhitespace)) currentToken.clear()
                else currentToken.append(char)
              }
            }
            inQuotes = !inQuotes
          }
          inField = !inField

        case _ if char == delimiterChar && !inQuotes ⇒
          tokensOnThisLine += currentToken.toString()
          currentToken.setLength(0)
          inField = false
        case _ ⇒
          if (!strictQuotes || inQuotes) {
            currentToken.append(char)
            inField = true
          }
      }
      position += 1
    }

    if (inQuotes) {
      if (multiLine) {
        currentToken.append("\n")
        pending = Some(currentToken.toString())
        currentToken.clear()
      }
      else {
        throw new IOException(s"Un-terminated quoted field at end of CSV line:\n${currentToken.toString()}")
      }
    }
    if (currentToken != null) {
      tokensOnThisLine += currentToken.toString()
    }
    tokensOnThisLine.toSeq
  }
}