package com.taj.supertaxlawyer.CommandLine

import org.slf4j.impl.SimpleLogger
import com.taj.supertaxlawyer.FileStructure.FileSizeTools
import java.io.File
import scala.io.Source
import scalaz._
import Scalaz._
import com.taj.supertaxlawyer.Extractor.LineExtractorActor

object ExecuteCommandLine {
  /**
   * Take a parsed command line object and execute the correct methods.
   * @param args arguments sent by the user to the application.
   */
  def apply(args: Array[String]) {
    val opts = CommandLineParser(args)

    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, if (opts.debug.get.getOrElse(false)) "debug" else "info")

    val optionOutput = opts.outputFolder.get

    val optionIncludeTitles = opts.excludeTitles.get

    val optionColumnSize = opts.columnSize.get

    val optionEncoding = opts.encoding.get

    optionColumnSize match {
      case Some(path) ⇒

        val includeTitles = optionIncludeTitles.getOrElse(false)
        val encoding = optionEncoding.getOrElse(FileSizeTools.detectEncoding(path))

        val (splitter, columnCount) = (opts.splitter.get, opts.columnCount.get) match {
          case (Some(tmpSplitter), Some(col)) ⇒ (tmpSplitter, col)
          case (Some(tmpSplitter), None) ⇒
            val replacedSplitter: String = tmpSplitter match {
              case "TAB"   ⇒ "\t"
              case "SPACE" ⇒ " "
              case c       ⇒ c
            }
            (replacedSplitter, FileSizeTools.columnCount(path, replacedSplitter, encoding))
          case _ ⇒ FileSizeTools.findColumnDelimiter(path, encoding)
        }

        val file = new File(path)
        val lines = Source.fromFile(path, encoding).getLines()
        val titles = if (includeTitles && lines.hasNext) lines.next().split(s"\\Q$splitter\\E").toList.some else None
        FileSizeTools.computeSize(file, splitter, columnCount, encoding, optionOutput, titles)
      case None ⇒
    }

    val (extractLinesOption) = (opts.startLine.get |@| opts.endLine.get).tupled

    (extractLinesOption, opts.extract.get) match {
      case (Some((firstLine, lastLine)), Some(filePath)) ⇒
        val encoding = optionEncoding.getOrElse(FileSizeTools.detectEncoding(filePath))
        LineExtractorActor.extract(filePath, encoding, opts.outputFile.get, firstLine, lastLine)
      case _ ⇒
    }
  }
}