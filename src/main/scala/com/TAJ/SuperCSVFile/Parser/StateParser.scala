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
        def parse(result: ParserValidation, remaining: Option[Int]): (ParserValidation,StringStack) = {
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

  val m2 = List("").traverseS(t ⇒ wordCountsForArticle(t))

  val m = List("").traverseS(t ⇒ wordCountsForArticle(t))

  val (wordMap, _) = m.run(Map.empty[String, Int])

}
