package com.TAJ.SuperCSVFile.Parser

import scala.collection.mutable.ArrayBuffer
import scalaz.{ Success, Failure }

case class ParserIterator(parser: OpenCSV, lines: Iterator[String]) extends Iterator[Seq[String]] {

  private def readNext: Seq[String] = {
    val result: ArrayBuffer[String] = ArrayBuffer()
    var pending: Option[String] = None
    do {
      Option(lines.next()) match {
        case None ⇒ return result // It was the last line, return empty array. May replace it by an option
        case Some(currentLineToParse) ⇒
          parser.parseLine(currentLineToParse, pending, hasNext) match {
            case (_, Failure(lineParsed)) ⇒
              result ++= lineParsed
            case (parsedPending, Success(lineParsed: Seq[String])) ⇒
              result ++= lineParsed
              pending = parsedPending
          }
      }
    } while (pending.isDefined && hasNext) // restart if pending

    result
  }

  override def hasNext: Boolean = lines.hasNext

  override def next(): Seq[String] = readNext
}
