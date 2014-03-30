package com.taj.supertaxlawyer.CommandLine

import org.slf4j.impl.SimpleLogger
import com.taj.supertaxlawyer.FileStructure.{ LineCounterActor, SizeActor, FileTools }
import java.io.File
import scala.io.Source
import scalaz._
import Scalaz._
import com.taj.supertaxlawyer.Extractor.LineExtractorActor
import akka.actor.ActorSystem
import com.taj.supertaxlawyer.{ Distributor, ActorContainer }
import com.taj.supertaxlawyer.ActorMessages.Start
import scala.collection.mutable.ArrayBuffer
import com.typesafe.scalalogging.slf4j.Logging

object ExecuteCommandLine extends Logging {
  /**
   * Take a parsed command line object and execute the correct methods.
   * @param args arguments sent by the user to the application.
   */
  def apply(args: Array[String]) {
    val opts = CommandLineParser(args)

    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, if (opts.debug.get.getOrElse(false)) "debug" else "info")

    implicit val system: ActorSystem = ActorSystem("ActorSystemComputation")

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

    linesCount match {
      case Some(true) ⇒
        listOfWorkers += LineCounterActor(outputLinesCount)
      case _ ⇒
    }

    actionColumnSize match {
      case Some(true) ⇒
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

        val lines = Source.fromFile(path, encoding).getLines()
        val titles = if (includeTitles && lines.hasNext) lines.next().split(s"\\Q$splitter\\E").toList.some else None

        listOfWorkers += SizeActor(outputColumnSize, columnCount, splitter, titles)
      case _ ⇒
    }

    actionExtract match {
      case Some(true) ⇒
        listOfWorkers += LineExtractorActor(outputExtract)
      case _ ⇒
    }

    val distributor = Distributor(file, encoding, listOfWorkers.toList, startLine, limitNumberOfLinesToRead = endLine)
    distributor ! Start()
  }
}