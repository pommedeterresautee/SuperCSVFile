package com.taj.supertaxlawyer.Extractor

import akka.actor.{ Props, ActorSystem, Actor }

import com.taj.supertaxlawyer.ActorContainer
import com.taj.supertaxlawyer.ActorMessages.{ RequestMoreWork, Lines }
import com.typesafe.scalalogging.slf4j.Logging

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
        case Some(filePath) ⇒
          File(filePath).appendAll(lines.mkString("\n"))
        case None ⇒ println(lines.mkString("\n"))
      }
      sender() ! RequestMoreWork()
  }

  override def postStop(): Unit = {
    logger.debug("*** Line extractor actor is dead. ***")
  }
}
