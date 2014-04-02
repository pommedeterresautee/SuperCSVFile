package com.taj.supertaxlawyer.CommandLine

import org.slf4j.impl.SimpleLogger
import com.taj.supertaxlawyer.FileStructure.{ LineCounterActor, SizeActor, FileTools }
import java.io.File
import scala.io.Source
import scalaz._
import Scalaz._
import com.taj.supertaxlawyer.Extractor.LineExtractorActor
import akka.actor.ActorSystem
import com.taj.supertaxlawyer.{ Reaper, Distributor, ActorContainer }
import com.taj.supertaxlawyer.ActorMessages.Start
import scala.collection.mutable.ArrayBuffer
import com.typesafe.scalalogging.slf4j.Logging
import com.taj.supertaxlawyer.ActorLife.RegisterMe

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

    val (splitter, columnCount) = (opts.setSplitter.get, opts.columnCount.get) match {
      case (Some(tmpSplitter), Some(col)) ⇒ (tmpSplitter, col)
      case (Some(tmpSplitter), None) ⇒
        val replacedSplitter: String = tmpSplitter match {
          case "TAB"   ⇒ "\t"
          case "SPACE" ⇒ " "
          case c       ⇒ c
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
        val titles = if (includeTitles && lines.hasNext) lines.next().split(s"\\Q$splitter\\E").toList.some else None
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
      case Some(outputDelimiterPath) ⇒ scala.reflect.io.File(outputDelimiterPath).writeAll(splitter)
      case None                      ⇒
    }

    val distributor = Distributor(file, encoding, listOfWorkers.toList, startLine, limitNumberOfLinesToRead = endLine)

    distributor ! Start()
  }
}