package com.taj.supertaxlawyer.Extractor

import akka.actor.{Props, ActorSystem, Actor}

import com.taj.supertaxlawyer.{ActorContainer, Distributor}
import java.io.File
import com.taj.supertaxlawyer.ActorMessages.{ReadNextBlock, Lines, Start}
import com.typesafe.scalalogging.slf4j.Logging


object LineExtractorActor {
  def apply(output: Option[String])(implicit system: ActorSystem) = system.actorOf(Props(new LineExtractorActor(output)), "ExtractLinesActor")

  /**
   * Extract lines from a file and write it to another file or print them to screen.
   * @param path to the original file.
   * @param encoding to the original file.
   * @param output optional path to the external file.
   * @param start begin the extraction with this line.
   * @param end finish the extraction at this line.
   */
  def extract(path: String, encoding: String, output: Option[String], start: Int, end: Int) = {
    output.map(new File(_)).filter(_.exists()).foreach(_.delete())
    implicit val system: ActorSystem = ActorSystem("ActorSystemExtraction")
    val file = new File(path)
    val extractor = ActorContainer(LineExtractorActor(output), isRooter = false)
    val listOfWorker = List(extractor)
    val actor = Distributor(file, encoding, listOfWorker, dropFirsLines = start, limitNumberOfLinesToRead = Some(end - start))
    actor ! Start()
  }
}

/**
 * Write the lines received in an output file.
 */
class LineExtractorActor(outputFile: Option[String]) extends Actor with Logging {

  import scala.reflect.io.File
  override def receive: Receive = {
    case Lines(lines, index) =>
      logger.debug(s"Received ${lines.size} lines.")
      outputFile match {
        case Some(filePath) =>
          File(filePath).appendAll(lines.mkString("\n"))
        case None => println(lines.mkString("\n"))
      }
      sender () ! ReadNextBlock()
  }

  override def postStop(): Unit = {
    logger.debug("*** Line extractor actor is dead. ***")
  }
}