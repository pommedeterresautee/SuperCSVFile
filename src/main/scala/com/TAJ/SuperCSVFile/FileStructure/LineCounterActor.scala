/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014. TAJ - Société d'avocats
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * EXCEPT AS CONTAINED IN THIS NOTICE, THE NAME OF TAJ - Société d'avocats SHALL
 * NOT BE USED IN ADVERTISING OR OTHERWISE TO PROMOTE THE SALE, USE OR OTHER
 * DEALINGS IN THIS SOFTWARE WITHOUT PRIOR WRITTEN AUTHORIZATION FROM
 * TAJ - Société d'avocats.
 */

package com.TAJ.SuperCSVFile.FileStructure

import akka.actor._
import scala.reflect.io.{ File, Path }
import com.typesafe.scalalogging.slf4j.{ LazyLogging }
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

  def apply(testActor: ActorRef, output: Option[String])(implicit system: ActorSystem): ActorContainer = {
    val actorTrait = new LineCounterActorComponent with ResultLineCounterActorComponent {
      override val resultActor = testActor
      override val linesActor = system.actorOf(Props(new LineCounterActor()), name = "CounterLineActor")
    }
    ActorContainer(actorTrait.linesActor, isRooter = false)
  }
}

trait LineCounterActorComponent extends LazyLogging {
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