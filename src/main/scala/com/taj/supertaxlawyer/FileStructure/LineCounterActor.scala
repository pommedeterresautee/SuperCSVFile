package com.taj.supertaxlawyer.FileStructure

import akka.actor.{ActorSystem, Props, Actor}
import com.taj.supertaxlawyer.ActorMessages.Lines
import com.taj.supertaxlawyer.ActorContainer

/**
 * Constructor for the line counter
 */
object LineCounterActor {

  def apply(system: ActorSystem, output: Option[String]): ActorContainer = ActorContainer(system.actorOf(Props(new LineCounterActor(output)), name = "LineCounter"), isRooter = false)
}

/**
 * Count lines in the analyzed text file.
 */
class LineCounterActor(output: Option[String]) extends Actor {
  var mTotalSize = 0

  override def receive: Actor.Receive = {
    case Lines(lines) =>
      println("received")
      mTotalSize += lines.size
  }

  override def postStop(): Unit = {
    output match {
      case None => println(s"The text file contains $mTotalSize lines")
      case Some(path) => //TODO add save to folder
    }
  }
}