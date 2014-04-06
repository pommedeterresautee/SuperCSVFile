package com.TAJ.SuperCSVFile.Extractor

import akka.actor.{ Props, ActorSystem, Actor }

import com.TAJ.SuperCSVFile.ActorContainer
import com.TAJ.SuperCSVFile.ActorMessages.{ RequestMoreWork, Lines }
import com.typesafe.scalalogging.slf4j.Logging
import scala.reflect.io.Path

object LineExtractorActor {
  def apply(output: Option[String])(implicit system: ActorSystem) = ActorContainer(system.actorOf(Props(new LineExtractorActor(output)), "ExtractLinesActor"), isRooter = false)
}

/**
 * Write the lines received in an output file.
 */
class LineExtractorActor(outputFile: Option[String]) extends Actor with Logging {

  import scala.reflect.io.File
  override def receive: Receive = {
    case Lines(lines, index) ⇒
      logger.debug(s"Received ${lines.size} lines.")
      outputFile match {
        case Some(filePath) if Path(filePath).isFile ⇒
          File(filePath).appendAll(lines.mkString("\n"))
        case Some(filePath) if !Path(filePath).isFile ⇒
          throw new IllegalArgumentException(s"Path provided is not a file: $filePath")
        case None ⇒ println(lines.mkString("\n"))
      }
      sender() ! RequestMoreWork()
  }

  override def postStop(): Unit = {
    logger.debug("*** Line extractor actor is dead. ***")
  }
}
