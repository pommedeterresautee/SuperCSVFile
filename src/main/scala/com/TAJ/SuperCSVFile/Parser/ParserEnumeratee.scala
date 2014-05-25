package com.TAJ.SuperCSVFile.Parser

import play.api.libs.iteratee.{ Done, Cont, Input, Iteratee }
import com.TAJ.SuperCSVFile.Parser.ParserTypes.{ ParserResult, ParserState }

case class ParserEnumeratee(DelimiterChar: Char = ',', QuoteChar: Char = '"', EscapeChar: Char = '\\', IgnoreCharOutsideQuotes: Boolean = false, IgnoreLeadingWhiteSpace: Boolean = true, BackParseLimit: Option[Int] = Some(1)) {

  val parser = OpenCSV(DelimiterChar, QuoteChar, EscapeChar, IgnoreCharOutsideQuotes, IgnoreLeadingWhiteSpace)

  //TODO change the \n
  val initState = ParserState.createInitialState("\n", parser, BackParseLimit)

  def parserIteratee(): Iteratee[String, Seq[ParserResult]] = {
      def step(originalParserState: ParserState, precedentLine: Option[String])(nextInput: Input[String]): Iteratee[String, Seq[ParserResult]] =
        nextInput match {
          case Input.El(line) if precedentLine.isEmpty ⇒ Cont[String, Seq[ParserResult]](inputLine ⇒ step(initState, Some(line))(inputLine))
          case Input.El(line) ⇒
            println("ligne precedente = " + precedentLine)
            val (state, newLine) = originalParserState.stack match {
              case Nil ⇒
                println("Stack vide")
                (originalParserState.copy(counter = originalParserState.counter + 1), precedentLine.get)

              case head :: tail ⇒
                println("Stack avec " + head + " et reste: " + tail)
                (originalParserState.copy(stack = tail), head)
            }

            val (newState, currentResult) = CSVLineParser.parseOneLine(state, newLine, hasNext = true) // has next est toujours true mais si on est plus dans remaining, est ce que ca reste vrai ? En principe oui car je suis une ligne en avance, si je suis dans ce bloc c est que je suis sur que je l ai voulu. Peut etre il faut mettre un guard dans le pattern matching pour savoir si il reste des tours.

            if (newState.PendingParsing.isDefined && newState.remaining.forall(_ >= 0)) {
              println("State: " + newState)
              //au lieu de mettre a variable line dans precedent, pourquoi ne pas la mettre dans le stack ?
              Cont[String, Seq[ParserResult]](inputLine ⇒ step(newState, Some(line))(inputLine))
            }
            else {
              //TODO Vider le stack avant de renvoyer un resultat
              //ajouter une variable pour suivre le resultat cumule (n est pas dans la variable etat)
              //mettre la ligne precedente dans le stack et virer la variable ???
              println("State end: " + newState)
              println("*** Result en cours *** " + currentResult)
              Done(Seq(currentResult), nextInput)
            }

          case _ ⇒
            val (newState, currentResult) = CSVLineParser.parseOneLine(originalParserState, precedentLine.get, hasNext = false)
            println("*** Result fin *** " + currentResult)
            Done(Seq(currentResult), nextInput)
        }

    Cont[String, Seq[ParserResult]](inputLine ⇒ step(initState, None)(inputLine))
  }

  def parseStack(s: ParserState): (Seq[ParserResult], ParserState) = {

    var sBis = s.copy(stack = Seq())
    val stack = s.stack
    val i = stack.toIterator
    var finalResult: Seq[ParserResult] = Seq()

    while (i.hasNext) {
      val (finalState, tempResult) = CSVLineParser.parseBlockOfLines(sBis, () ⇒ i.next(), () ⇒ i.hasNext)
      println(tempResult)
      finalResult :+= tempResult
      sBis = finalState
    }

    (finalResult, sBis)
  }
}