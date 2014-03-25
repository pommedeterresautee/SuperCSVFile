package com.taj.supertaxlawyer.Extractor

import akka.actor.{Props, ActorSystem, Actor}
import scala.reflect.io.File


object LineExtractorActor {
  def apply(output: Option[String])(implicit system: ActorSystem) = system.actorOf(Props(new LineExtractorActor(output)), "ExtractLinesActor")

  def extract() = {

  }
}

/**
 * Write the lines received in an output file.
 */
class LineExtractorActor(outputFile: Option[String]) extends Actor {
  override def receive: Receive = {
    case lines: List[String] =>
      outputFile match {
        case Some(filePath) =>
          File(filePath + self.path.name).appendAll(lines.mkString("\n"))
        case None => println(lines.mkString("\n"))
      }
  }
}