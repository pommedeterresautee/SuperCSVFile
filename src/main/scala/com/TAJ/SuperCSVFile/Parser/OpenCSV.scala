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

/**
 * This block of code is inspired from OpenCSV library.
 *
 * Constructs CSVReader with supplied separator and quote char.
 * Allows setting the "strict quotes" and "ignore leading whitespace" flags
 *
 * @param delimiter               the delimiter to use for separating entries
 * @param quoteChar               the character to use for quoted elements
 * @param escape                  the character to use for escaping a separator or quote
 * @param strictQuotes            if true, characters outside the quotes are ignored
 * @param ignoreLeadingWhiteSpace if true, white space in front of a quote in a field is ignored
 */
case class OpenCSV(delimiter: Char = ',', quoteChar: Char = '"', escape: Char = '\\', strictQuotes: Boolean = false, ignoreLeadingWhiteSpace: Boolean = true) {
  require(!anyCharactersAreTheSame(), "Some or all of the parameters of the CSV parser are equal.")

  private var pending: Option[String] = None
  private var inField: Boolean = false

  /**
   * The default separator to use if none is supplied to the constructor.
   */
  private val INITIAL_READ_SIZE: Int = 128

  private def anyCharactersAreTheSame(): Boolean = {
    val NULL_CHARACTER: Char = '\0'
      def isSameCharacter(c1: Char, c2: Char): Boolean = {
        c1 != NULL_CHARACTER && c1 == c2
      }
    isSameCharacter(delimiter, quoteChar) || isSameCharacter(delimiter, escape) || isSameCharacter(quoteChar, escape)
  }

  def parseLineMulti(nextLine: String): Seq[String] = parseLine(nextLine, multi = true)

  def parseLine(nextLine: String): Seq[String] = parseLine(nextLine, multi = false)

  /**
   * Parses an incoming String and returns an array of elements.
   *
   * @param nextLine the string to parse
   * @param multi true if we are parsing multiple raw lines for the same CSV line
   * @return the comma-tokenized list of elements, or null if nextLine is null
   * @throws IOException if bad things happen during the read
   */
  private def parseLine(nextLine: String, multi: Boolean): Seq[String] = {
    val tokensOnThisLine: ArrayBuffer[String] = ArrayBuffer()
    val sb: StringBuilder = new StringBuilder(INITIAL_READ_SIZE)
    var inQuotes: Boolean = false
    var counter: Int = 0

    if (!multi && pending.isDefined) {
      pending = None
    }
    if (nextLine == null) {
      if (pending.isDefined) {
        val s: String = pending.get
        pending = None
        return Array[String](s)
      }
      else {
        return null
      }
    }

    if (pending.isDefined) {
      sb.append(pending.get)
      pending = None
      inQuotes = true
    }

    while (counter < nextLine.length) {
      val char: Char = nextLine(counter)

      char match {
        case _ if char == escape ⇒
          if (isNextCharacterEscapable(nextLine, inQuotes || inField, counter)) {
            sb.append(nextLine(counter + 1))
            counter += 1
          }
        case _ if char == quoteChar ⇒
          if (isNextCharacterEscapedQuote(nextLine, inQuotes || inField, counter)) {
            sb.append(nextLine(counter + 1))
            counter += 1
          }
          else {
            if (!strictQuotes) {
              if (counter > 2 && nextLine(counter - 1) != delimiter && nextLine.length > (counter + 1) && nextLine(counter + 1) != delimiter) {
                if (ignoreLeadingWhiteSpace && sb.length > 0 && isAllWhiteSpace(sb)) {
                  sb.setLength(0)
                }
                else {
                  sb.append(char)
                }
              }
            }
            inQuotes = !inQuotes
          }
          inField = !inField

        case _ if char == delimiter && !inQuotes ⇒
          tokensOnThisLine += sb.toString()
          sb.setLength(0)
          inField = false
        case _ ⇒
          if (!strictQuotes || inQuotes) {
            sb.append(char)
            inField = true
          }
      }
      counter += 1
    }

    if (inQuotes) {
      if (multi) {
        sb.append("\n")
        pending = Some(sb.toString())
        sb.clear()
      }
      else {
        throw new IOException(s"Un-terminated quoted field at end of CSV line:\n${sb.toString()}")
      }
    }
    if (sb != null) {
      tokensOnThisLine += sb.toString()
    }
    tokensOnThisLine.toSeq
  }

  /**
   * precondition: the current character is a quote or an escape
   *
   * @param nextLine the current line
   * @param inQuotes true if the current context is quoted
   * @param i        current index in line
   * @return true if the following character is a quote
   */
  private def isNextCharacterEscapedQuote(nextLine: String, inQuotes: Boolean, i: Int): Boolean = inQuotes && nextLine.length > (i + 1) && nextLine(i + 1) == quoteChar

  /**
   * precondition: the current character is an escape
   *
   * @param nextLine the current line
   * @param inQuotes true if the current context is quoted
   * @param i        current index in line
   * @return true if the following character is a quote
   */
  private def isNextCharacterEscapable(nextLine: String, inQuotes: Boolean, i: Int): Boolean = inQuotes && nextLine.length > (i + 1) && (nextLine(i + 1) == quoteChar || nextLine(i + 1) == escape)

  /**
   * precondition: sb.length() > 0
   *
   * @param sb A sequence of characters to examine
   * @return true if every character in the sequence is whitespace
   */
  private def isAllWhiteSpace(sb: CharSequence): Boolean = sb.toString.toCharArray.forall(!Character.isWhitespace(_))
}