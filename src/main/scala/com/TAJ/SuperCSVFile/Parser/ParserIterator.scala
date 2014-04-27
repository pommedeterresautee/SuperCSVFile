package com.TAJ.SuperCSVFile.Parser

import scalaz._

case class ParserIterator(parser: OpenCSV, lines: Iterator[String]) extends Iterator[Seq[String]] {

  override def hasNext: Boolean = lines.hasNext

  override def next(): Seq[String] = {
    var result: Seq[String] = Seq()
    var pending: Option[String] = None
    do {
      parser.parseLine(lines.next(), pending, hasNext) match {
        case (_, Failure(lineParsed)) ⇒
          result ++= lineParsed
        case (parsedPending, Success(lineParsed: Seq[String])) ⇒
          result ++= lineParsed
          pending = parsedPending
      }
    } while (pending.isDefined && hasNext) // restart if pending
    result
  }
}
