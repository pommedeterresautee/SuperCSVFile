package com.TAJ.SuperCSVFile.Parser

import scalaz.Validation

object ParserType {
  type ParserResult[A] = Validation[Seq[A], Seq[A]]
  type StateParsing[A] = (Option[A], ParserResult[A])
}
