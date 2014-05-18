package com.TAJ.SuperCSVFile.Parser

import play.api.libs.iteratee.{ Done, Cont, Input, Iteratee }
import com.TAJ.SuperCSVFile.Parser.ParserTypes.{ FailedParser, ParserResult, ParserState }

case class ParserEnumeratee(DelimiterChar: Char = ',', QuoteChar: Char = '"', EscapeChar: Char = '\\', IgnoreCharOutsideQuotes: Boolean = false, IgnoreLeadingWhiteSpace: Boolean = true, BackParseLimit: Option[Int] = Some(1)) {

  val parser = OpenCSV(DelimiterChar, QuoteChar, EscapeChar, IgnoreCharOutsideQuotes, IgnoreLeadingWhiteSpace)

  val initState = ParserState.createInitialState("\n", parser, Some(1))

  def parserIteratee(): Iteratee[String, ParserResult] = {
      def step(originalParserState: ParserState)(nextInput: Input[String]): Iteratee[String, ParserResult] =
        nextInput match {
          case Input.El(line) ⇒
            val (newState, currentResult) = CSVLineParser.parseOneLine(originalParserState, 0, line, Seq(), hasNext = false)
            println("this is the current line: " + currentResult)
            Done(currentResult, Input.Empty)
          case Input.Empty ⇒
            println("dead empty")
            Done(FailedParser(Seq(), "", 0), Input.Empty)
          case Input.EOF ⇒
            println("dead end")
            Done(FailedParser(Seq(), "", 0), Input.EOF)
        }
    Cont[String, ParserResult](inputLine ⇒ step(initState)(inputLine))
  }

  //    @tailrec
  //    private def parse(originalParserState: ParserState): (ParserState, ParserResult) = {
  //      val (rawFileLineCounter, newLine, stack) = originalParserState.stack match {
  //        case Nil          ⇒ (originalParserState.counter + 1, IteratorOfLines.next(), originalParserState.stack)
  //        case head :: tail ⇒ (originalParserState.counter, head, tail)
  //      }
  //      val (newState, currentResult) = CSVLineParser.parseOneLine(originalParserState, rawFileLineCounter, newLine, stack, IteratorOfLines.hasNext)
  //      if (newState.PendingParsing.isDefined && IteratorOfLines.hasNext && newState.remaining.forall(_ >= 0)) parse(newState)
  //      else {
  //        val finalState = ParserState.newStateForNextIteration(newState)
  //        (finalState, currentResult)
  //      }
  //    }

}
