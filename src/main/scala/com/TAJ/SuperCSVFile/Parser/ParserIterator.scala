package com.TAJ.SuperCSVFile.Parser

import scalaz._

case class ParserIterator(parser: OpenCSV, lines: Iterator[String]) extends Iterator[Seq[String]] {

  private def readNext: Seq[String] = {
    var result: Seq[String] = Seq()
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
