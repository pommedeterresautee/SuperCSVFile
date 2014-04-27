package com.TAJ.SuperCSVFile.Parser

import scalaz.Validation
import scala.collection.mutable.ArrayBuffer

case class CSVTraversable(filePath: String, delimiterChar: Char = ',', quoteChar: Char = '"', escapeChar: Char = '\\', ignoreCharOutsideQuotes: Boolean = false, ignoreLeadingWhiteSpace: Boolean = true) extends Traversable[Validation[Seq[String], Seq[String]]] {

  private val source = io.Source.fromFile(filePath).getLines()

  private val parser = OpenCSV(delimiterChar, quoteChar, escapeChar, ignoreCharOutsideQuotes)

  override def foreach[U](f: (Validation[Seq[String], Seq[String]]) ⇒ U): Unit = {

    while (source.hasNext) {
      f(parser.parseLine(source.next()))
    }
  }
}