/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014. TAJ - Société d'avocats
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * EXCEPT AS CONTAINED IN THIS NOTICE, THE NAME OF TAJ - Société d'avocats SHALL
 * NOT BE USED IN ADVERTISING OR OTHERWISE TO PROMOTE THE SALE, USE OR OTHER
 * DEALINGS IN THIS SOFTWARE WITHOUT PRIOR WRITTEN AUTHORIZATION FROM
 * TAJ - Société d'avocats.
 */

package com.TAJ.SuperCSVFile.CommandLine

import org.slf4j.impl.SimpleLogger
import com.TAJ.SuperCSVFile.FileStructure.{ LineCounterActor, SizeActor, FileTools }
import java.io.File
import scala.io.Source
import scalaz._
import Scalaz._
import com.TAJ.SuperCSVFile.Extractor.LineExtractorActor
import akka.actor.ActorSystem
import com.TAJ.SuperCSVFile.{ Reaper, Distributor, ActorContainer }
import scala.collection.mutable.ArrayBuffer
import com.typesafe.scalalogging.slf4j.Logging
import com.TAJ.SuperCSVFile.ActorLife.RegisterMe
import com.TAJ.SuperCSVFile.ActorMessages.RequestMoreWork
import com.TAJ.SuperCSVFile.Parser.OpenCSV

object ExecuteCommandLine extends Logging {
  /**
   * Take a parsed command line object and execute the correct methods.
   * @param args arguments sent by the user to the application.
   */
  def apply(args: Array[String]) {
    val opts = CommandLineParser(args)

    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, if (opts.debug.get.getOrElse(false)) "debug" else "info")

    implicit val system: ActorSystem = ActorSystem("ActorSystemComputation")

    val reaper = Reaper()

    val listOfWorkers: ArrayBuffer[ActorContainer] = ArrayBuffer()

    val path = opts.inputFiles.get match {
      case Some(pathToFile) ⇒ pathToFile
      case _                ⇒ throw new IllegalArgumentException("Input file not provided!")
    }

    val file = new File(path)

    val actionColumnSize = opts.columnSize.get
    val encoding = opts.setEncoding.get.getOrElse(FileTools.detectEncoding(path))
    val outputColumnSize = opts.outputColumnSizes.get
    val includeTitles = opts.excludeTitles.get.getOrElse(false)

    val actionExtract = opts.extract.get
    val outputExtract = opts.outputExtractLines.get

    val outputDelimiter = opts.outputDelimiter.get

    val startLine = opts.startLine.get
    val endLine = opts.lastLine.get

    val linesCount = opts.linesCount.get
    val outputLinesCount = opts.outputLinesCount.get

    val (splitter: Char, columnCount) = (opts.setSplitter.get, opts.columnCount.get) match {
      case (Some(tmpSplitter), Some(col)) ⇒ (tmpSplitter, col)
      case (Some(tmpSplitter), None) ⇒
        val replacedSplitter: Char = tmpSplitter match {
          case "TAB"   ⇒ '\t'
          case "SPACE" ⇒ ' '
          case c       ⇒ c.toCharArray.head // escape special characters (in particular pipe (|))
        }
        (replacedSplitter, FileTools.columnCount(path, replacedSplitter, encoding))
      case _ ⇒ FileTools.findColumnDelimiter(path, encoding)
    }

    linesCount match {
      case Some(true) ⇒
        val actor = LineCounterActor(outputLinesCount)
        listOfWorkers += actor
        reaper ! RegisterMe(actor.actor)
      case _ ⇒
    }

    actionColumnSize match {
      case Some(true) ⇒
        val lines = Source.fromFile(path, encoding).getLines()
        val titles = if (includeTitles && lines.hasNext) OpenCSV(delimiter = splitter).parseLine(lines.next()).toList.some else None
        val (computer, resultAccumulator, finalResult) = SizeActor(outputColumnSize, columnCount, splitter, titles)
        listOfWorkers += computer
        reaper ! RegisterMe(computer.actor)
        reaper ! RegisterMe(resultAccumulator)
        reaper ! RegisterMe(finalResult)
      case _ ⇒
    }

    actionExtract match {
      case Some(true) ⇒
        val actor = LineExtractorActor(outputExtract)
        listOfWorkers += actor
        reaper ! RegisterMe(actor.actor)
      case _ ⇒
    }

    outputDelimiter match {
      case Some(outputDelimiterPath) ⇒ scala.reflect.io.File(outputDelimiterPath).writeAll(splitter.toString)
      case None                      ⇒
    }

    val distributor = Distributor(file, encoding, listOfWorkers.toList, startLine, limitNumberOfLinesToRead = endLine)
    reaper ! RegisterMe(distributor)

    distributor ! RequestMoreWork()
  }
}