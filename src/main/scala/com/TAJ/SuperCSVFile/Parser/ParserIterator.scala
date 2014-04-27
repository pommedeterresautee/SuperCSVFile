package com.TAJ.SuperCSVFile.Parser

import scalaz._
import scala.collection.mutable

case class ParserIterator(parser: OpenCSV, lines: Iterator[String], limit: Option[Int] = Some(1)) extends Iterator[Seq[String]] {
  require(limit.getOrElse(1) >= 0, "Limit of the Iterator should be > 0")

  var LineStack: mutable.Stack[String] = mutable.Stack()

  override def hasNext: Boolean = lines.hasNext || LineStack.size > 0

  override def next(): Seq[String] = {
    var remaining = limit.getOrElse(1)
    var result: Seq[String] = Seq()
    var pending: Option[String] = None
    do {
      parser.parseLine(getNextLine, pending, hasNext) match {
        case (_, Failure(lineParsed)) ⇒
          result ++= lineParsed
        case (parsedPending, Success(lineParsed: Seq[String])) if remaining == 0 ⇒
          parsedPending.getOrElse("").split("\n").toList match {
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

  private def getNextLine = {
    if (LineStack.size > 0) LineStack.pop()
    else lines.next()
  }
}
