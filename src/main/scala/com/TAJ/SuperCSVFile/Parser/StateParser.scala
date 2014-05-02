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

import scalaz._
import Scalaz._

case class StateParser(DelimiterChar: Char = ',', QuoteChar: Char = '"', EscapeChar: Char = '\\', IgnoreCharOutsideQuotes: Boolean = false, IgnoreLeadingWhiteSpace: Boolean = true, IteratorOfLines: Iterator[String], BackParseLimit: Option[Int] = Some(1)) {
  require(BackParseLimit.getOrElse(1) >= 0 && BackParseLimit.getOrElse(1) < 10000, "Limit of the Iterator should be > 0 and < 10 000 for memory reasons")

  private val eol = System.getProperty("line.separator")

  private val parser = OpenCSV(DelimiterChar, QuoteChar, EscapeChar, IgnoreCharOutsideQuotes, IgnoreLeadingWhiteSpace)

  val decreaseUnit = Some(1)

  def something() = {
    State { i: Int ⇒ (i, i) }
  }

  def words(s: String) = Seq(): Seq[String]

  def wordCounts(str: String): State[Map[String, Int], Unit] = modify {
    currMap: Map[String, Int] ⇒
      words(str).foldLeft(currMap) { (map, word) ⇒
        val count = map.getOrElse(word, 0) + 1
        map + (word -> count)
      }
  }

  def wordCountsForArticle(s: String): State[Map[String, Int], Unit] = for {
    _ ← wordCounts(s)
    _ ← wordCounts(s)
    _ ← wordCounts(s)
  } yield ()

  def whatever() = State[ParserState, ParserValidation] {
    case (iterator, stack: mutable.Stack[String]) ⇒
      def hasNext: Boolean = iterator.hasNext || !stack.isEmpty

        @tailrec
        def parse(result: ParserValidation, remaining: Option[Int]): (ParserValidation, StringStack) = {
          var stackk = stack
          val nextLine = if (!stack.isEmpty) stack.pop() else IteratorOfLines.next()
          val currentResult: ParserValidation = parser.parseLine(nextLine, result.PendingParsing, hasNext && !remaining.exists(_ < 0) /*add remaining test here because for the parser it is the last line to parse even if there are more in the file.*/ ) match {
            case FailedParsing(failedMultipleLine) ⇒
              val lineParsed: Seq[String] = failedMultipleLine.head.split(eol, -1).toList
              stackk ++= lineParsed.tail
              FailedParsing((result.ParsedLine :+ lineParsed.head) ++ failedMultipleLine.tail)
            case line: SuccessParsing ⇒ line
            case PendingParsing(parsedPending, lineParsed) if remaining contains 0 ⇒
              parsedPending.split(eol, -1).toList match {
                case head :: tail ⇒
                  stackk ++= tail
                  FailedParsing(lineParsed :+ head)
                case Nil ⇒ SuccessParsing(lineParsed)
              }
            case PendingParsing(parsedPending, lineParsed) ⇒ PendingParsing(parsedPending, result.ParsedLine ++ lineParsed)
          }

          val newRemaining = (BackParseLimit |@| decreaseUnit) { _ - _ }

          if (currentResult.isPending && hasNext && !newRemaining.exists(_ < 0)) parse(currentResult, newRemaining)
          else (currentResult, stackk)
        }

      val (r, stackk) = parse(SuccessParsing(Seq()), BackParseLimit)

      ((iterator, stackk), r)
  }

  val m1 = for {
    s0 ← get[String]
    _ ← put(s0 * s0.size)
    s1 ← get[String]
  } yield s1.size

  val t = m1.run("hello")

  val it = EphemeralStream.fromStream(List("").toStream)

  val m2 = it.traverseS(t ⇒ wordCountsForArticle(t))

  val m = List("").traverseS(t ⇒ wordCountsForArticle(t))

  val (wordMap, _) = m.run(Map.empty[String, Int])

}
