package com.TAJ.SuperCSVFile

import scala.annotation.tailrec

/**
 * Parse CSV file.
 */
object CSVParser extends App {

  @tailrec
  def parseLines(linesToProcess: Seq[String], linesProcessed: Seq[Seq[String]]): Seq[Seq[String]] = {
    linesToProcess match {
      case Nil ⇒ linesProcessed
      case head :: tail ⇒
        val line = parseLine(head, Seq(), Seq(), ',', '"') match {
          case (result, Nil)       ⇒ result
          case (result, toProcess) ⇒ ???
          //            val (good, toFinish) = parseQuotes(toProcess, Seq(), ',', '"')
          //            val remainingLines = toFinish.toString().split("\n").toSeq
          //            val result = parseLines(remainingLines, Seq())
          //            val t = good:+result
        }
        parseLines(tail, linesProcessed :+ line)
    }
  }

  @tailrec
  def parseLine(toProcess: Seq[Char], currentWord: Seq[Char], parsedLine: Seq[String], delimiter: Char, quote: Char): (Seq[String], Seq[Char]) = {
    toProcess match {
      case Nil ⇒ (parsedLine :+ currentWord.mkString, Nil)
      case Seq(head, next, then, after, tail @ _*) if (head == delimiter && next == quote && then != quote) ||
        (head == delimiter && next == quote && then == quote && after == quote) ⇒ (parsedLine :+ currentWord.mkString, toProcess)
      case Seq(head, next, tail @ _*) if head == quote && next == quote ⇒
        parseLine(tail, currentWord :+ head, parsedLine, delimiter, quote)
      case Seq(head, tail @ _*) if head == delimiter ⇒
        parseLine(tail, Seq(), parsedLine :+ currentWord.mkString, delimiter, quote)
      case Seq(head, tail @ _*) ⇒
        parseLine(tail, currentWord :+ head, parsedLine, delimiter, quote)
    }
  }

  @tailrec
  def parseQuotes(toProcess: Seq[Char], quotePart: Seq[Char], delimiter: Char, quote: Char): (Seq[Char], Seq[Char]) = {
    toProcess match {
      case Nil ⇒ (Seq(), Nil)
      case Seq(head, next, tail @ _*) if head == quote && next == quote ⇒
        parseQuotes(tail, quotePart :+ head, delimiter, quote)
      case Seq(head, next, tail @ _*) if head == quote && next == delimiter ⇒
        (quotePart, tail)
      case Seq(head, tail @ _*) ⇒
        parseQuotes(tail, quotePart :+ head, delimiter, quote)
    }
  }

  val chars = Seq("toto, tata, tutu,haha, \"fckjdhsakfjhkjsdhf\"\"", "hoho,hihi,koko,kiki")
  print(parseLines(chars, Seq()).mkString("\n"))

  val test = """ceci, est, ""haha""
               |et encore, fdsgf
               |kldjask", tgjfdksglj
             """.stripMargin

  println(parseQuotes(test, Seq(), ',', '"'))

}
