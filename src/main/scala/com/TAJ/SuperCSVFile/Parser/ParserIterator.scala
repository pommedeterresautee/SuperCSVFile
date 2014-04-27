package com.TAJ.SuperCSVFile.Parser

import scala.collection.mutable.ArrayBuffer
import scalaz.{ Success, Failure }

case class ParserIterator(parser: OpenCSV, lines: Iterator[String]) {

  def readAll(): Seq[Seq[String]] = ???

  def readNext: Seq[String] = {
    val result: ArrayBuffer[String] = ArrayBuffer()
    do {
      getNextLine match {
        case None ⇒ return result // It was the last line, return empty array. May replace it by an option
        case Some(nextLine) ⇒
          parser.parseLineMulti(nextLine) match {
            case Failure(lineParsed) ⇒ result ++= lineParsed
            case Success(lineParsed) ⇒ result ++= lineParsed
          }
      }
    } while (parser.hasPartialReading) // restart if pending
    result
  }

  private def getNextLine: Option[String] = Option(lines.next())

}
