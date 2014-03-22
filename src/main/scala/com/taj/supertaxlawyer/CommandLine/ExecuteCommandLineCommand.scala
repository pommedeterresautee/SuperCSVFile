package com.taj.supertaxlawyer.CommandLine

import org.slf4j.impl.SimpleLogger
import com.taj.supertaxlawyer.FileStructure.FileSizeTools
import java.io.File
import scala.io.Source

/**
 * Take a parsed command line object and execute the correct methods.
 */
object ExecuteCommandLineCommand {
  def apply(args: Array[String]) {
    val opts = CommandLineParser(args)

    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, if (opts.debug.get.getOrElse(false)) "debug" else "info")

    val optionColumnCount = opts.columnCount.get
    val optionSplitter = opts.splitter.get
    val optionOutput = opts.output.get

    val optionIncludeTitles = opts.excludeTitles.get

    val optionColumnSize = opts.columnSize.get

    val optionEncoding = opts.encoding.get

    optionColumnSize match {
      case Some(path) =>
        val splitter: String = optionSplitter match {
          case Some("TAB") => "\t"
          case Some("SPACE") => " "
          case Some(s: String) => s
          case None => throw new IllegalArgumentException("No splitter provided.") // impossible in theory because blocked by ScalaOp
        }

        val includeTitles = optionIncludeTitles.getOrElse(false)
        val encoding = optionEncoding.getOrElse(FileSizeTools.detectEncoding(path))
        val columnCount = optionColumnCount.getOrElse(FileSizeTools.columnCount(path, splitter, encoding))

        val file = new File(path)
        val lines = Source.fromFile(path, encoding).getLines()
        val titles = if (includeTitles && lines.hasNext) Some(lines.next().split(s"\\Q$splitter\\E").toList) else None
        FileSizeTools.computeSize(file, splitter, columnCount, encoding, optionOutput, titles)
      case _ =>
    }
  }
}