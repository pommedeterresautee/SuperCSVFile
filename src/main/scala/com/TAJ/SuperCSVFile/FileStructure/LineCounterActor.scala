package com.TAJ.SuperCSVFile.FileStructure

import akka.actor._
import com.TAJ.SuperCSVFile.ActorMessages.Lines
import akka.testkit.TestProbe
import scala.reflect.io.{ File, Path }
import com.typesafe.scalalogging.slf4j.Logging
import com.TAJ.SuperCSVFile.ActorContainer
import com.TAJ.SuperCSVFile.ActorMessages.RequestMoreWork
import com.TAJ.SuperCSVFile.ActorMessages.JobFinished
import com.TAJ.SuperCSVFile.ActorMessages.Lines
import scala.Some

/**
 * Constructor for the line counter
 */
object LineCounterActor {

  def apply(output: Option[String])(implicit system: ActorSystem): ActorContainer = {
    val actorTrait = new LineCounterActorComponent with ResultLineCounterActorComponent {
      override val resultActor = system.actorOf(Props(new ResultLinesActor(output)), "ResultLinesActor")
      override val linesActor = system.actorOf(Props(new LineCounterActor()), name = "CounterLineActor")
    }
    ActorContainer(actorTrait.linesActor, isRooter = false)
  }
}

object LineCounterActorTest {

  def apply(testActor: TestProbe, output: Option[String])(implicit system: ActorSystem): ActorContainer = {
    val actorTrait = new LineCounterActorComponent with ResultLineCounterActorComponent {
      override val resultActor = testActor.ref
      override val linesActor = system.actorOf(Props(new LineCounterActor()), name = "CounterLineActor")
    }
    ActorContainer(actorTrait.linesActor, isRooter = false)
  }
}

trait LineCounterActorComponent extends Logging {
  self: ResultLineCounterActorComponent ⇒
  val linesActor: ActorRef

  /**
   * Count lines in the analyzed text file.
   */
  class LineCounterActor() extends Actor {
    var mTotalSize = 0l

    override def receive: Actor.Receive = {
      case Lines(lines, index) ⇒
        mTotalSize += lines.size
        sender() ! RequestMoreWork()
      case JobFinished() ⇒
        resultActor ! mTotalSize
        self ! PoisonPill
    }

    override def postStop(): Unit = {
      logger.debug(s"*** Actor ${self.path.name} is dead. ***")
    }
  }
}

trait ResultLineCounterActorComponent {
  val resultActor: ActorRef

  class ResultLinesActor(output: Option[String]) extends Actor {

    override def receive: Actor.Receive = {
      case mTotalSize: Long ⇒
        output match {
          case None ⇒ println(s"The text file contains $mTotalSize lines")
          case Some(outputPath) if Path(outputPath).isFile ⇒
            File(outputPath).writeAll(mTotalSize.toString)
          case Some(outputPath) ⇒ throw new IllegalArgumentException(s"Path provided is not to a file: $outputPath.")
        }
    }
  }
}