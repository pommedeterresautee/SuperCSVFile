package com.taj.supertaxlawyer.FileStructure

import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import com.taj.supertaxlawyer.ActorMessages.Lines
import com.taj.supertaxlawyer.ActorContainer
import akka.testkit.TestProbe
import scala.reflect.io.{File, Path}

/**
 * Constructor for the line counter
 */
object LineCounterActor {

  def apply(output: Option[String])(implicit system: ActorSystem): ActorContainer = {
    val actorTrait = new LineCounterActorComponent with ResultLineCounterActorComponent {
      override val resultActor = system.actorOf(Props(new ResultLinesActor(output)), "ResultLinesActor")
      override val linesActor = system.actorOf(Props(new LineCounterActor()), name = "LinesActor")
    }
    ActorContainer(actorTrait.linesActor, isRooter = false)
  }
}

object LineCounterActorTest {

  def apply(testActor: TestProbe, output: Option[String])(implicit system: ActorSystem): ActorContainer = {
    val actorTrait = new LineCounterActorComponent with ResultLineCounterActorComponent {
      override val resultActor = testActor.ref
      override val linesActor = system.actorOf(Props(new LineCounterActor()), name = "LinesActor")
    }
    ActorContainer(actorTrait.linesActor, isRooter = false)
  }
}


trait LineCounterActorComponent {
  self: ResultLineCounterActorComponent =>
  val linesActor: ActorRef

  /**
   * Count lines in the analyzed text file.
   */
  class LineCounterActor() extends Actor {
    var mTotalSize = 0l

    override def receive: Actor.Receive = {
      case Lines(lines) =>
        mTotalSize += lines.size
    }

    override def postStop(): Unit = {
      resultActor ! mTotalSize
    }
  }
}

trait ResultLineCounterActorComponent {
  val resultActor: ActorRef

  class ResultLinesActor(output: Option[String]) extends Actor {

    override def receive: Actor.Receive = {
      case mTotalSize: Long =>
        output match {
          case None => println(s"The text file contains $mTotalSize lines")
          case Some(outputPath) =>
            if (Path(outputPath).isDirectory) File(outputPath + self.path.name).writeAll(mTotalSize.toString)
            else throw new IllegalArgumentException(s"Path provided is not to a folder: $outputPath.")
        }
    }
  }
}