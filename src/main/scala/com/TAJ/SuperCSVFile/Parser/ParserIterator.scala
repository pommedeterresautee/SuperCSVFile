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

import scalaz._
import scala.collection.mutable

case class ParserIterator(parser: OpenCSV, lines: Iterator[String], limit: Option[Int] = Some(1)) extends Iterator[Seq[String]] {
  require(limit.getOrElse(1) >= 0 && limit.getOrElse(1) < 10000, "Limit of the Iterator should be > 0 and < 10 000 for memory reasons")

  val eol = System.getProperty("line.separator")

  var LineStack: mutable.Stack[String] = mutable.Stack()

  override def hasNext: Boolean = lines.hasNext || LineStack.size > 0

  override def next(): Seq[String] = {
    var remaining = limit.getOrElse(1)
    var result: Seq[String] = Seq()
    var pending: Option[String] = None
    do {
      parser.parseLine(getNextLine, pending, hasNext && remaining > 0 /*add remaining test here because for the parser it is the last line to parse even if there are more in the file.*/ ) match {
        case (_, Failure(failedLine)) ⇒
          pending = None
          val lineParsed: Seq[String] = failedLine.head.split(eol, 0).toList
          result ++= Seq(lineParsed.head)
          result ++= failedLine.tail
          LineStack ++= (lineParsed.tail)
        case (parsedPending, Success(lineParsed: Seq[String])) if remaining == 0 ⇒
          parsedPending.getOrElse("").split(eol, 0).toList match {
            case head :: tail if head == "" ⇒ result = lineParsed
            case head :: tail ⇒
              result = lineParsed :+ head
              LineStack ++= tail
            case Nil ⇒ result = lineParsed
          }
        case (parsedPending, Success(lineParsed: Seq[String])) ⇒
          result ++= lineParsed
          pending = parsedPending
      }
      if (limit.isDefined) remaining -= 1
    } while (pending.isDefined && hasNext && remaining >= 0) // restart if pending
    result
  }

  private def getNextLine = if (LineStack.size > 0) LineStack.pop() else {
    val res = lines.next()
    println("next: [" + res + "]")
    res
  }
}
