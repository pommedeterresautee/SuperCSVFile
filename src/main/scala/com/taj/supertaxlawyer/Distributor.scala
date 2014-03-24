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

package com.taj.supertaxlawyer

import scala.io.{Source, Codec}
import akka.actor._
import scala.collection.mutable.ArrayBuffer
import com.taj.supertaxlawyer.ActorMessages.RegisterMe
import com.taj.supertaxlawyer.ActorMessages.ReadNextBlock
import akka.actor.Terminated
import com.taj.supertaxlawyer.ActorMessages.Lines
import com.taj.supertaxlawyer.ActorMessages.Start
import com.taj.supertaxlawyer.ActorMessages.RegisterYourself
import akka.routing.Broadcast
import java.io.File
import com.typesafe.scalalogging.slf4j.Logging


case class ActorContainer(actor: ActorRef, isRooter: Boolean)

object Distributor {

  def apply(file: File, splitter: String, columnNumberExpected: Int, encoding: String, workers: List[ActorContainer], dropFirsLines: Int = 0, stopSystemAtTheEnd: Boolean = true, numberOfLinesPerMessage: Int = 500, suffixNameForTest: String = "", limitNumberOfLinesToRead: Option[Int] = None)(implicit system: ActorSystem) = system.actorOf(Props(new Distributor(file.getAbsolutePath, splitter, columnNumberExpected, encoding, workers, dropFirsLines, stopSystemAtTheEnd, numberOfLinesPerMessage, limitNumberOfLinesToRead)), name = s"Distributor_${file.getName}_$suffixNameForTest")
}

/**
 * Read the file and send the work.
 * @param path path to the file to analyze.
 * @param columnNumberExpected expected number of columns.
 */
class Distributor(path: String, splitter: String, columnNumberExpected: Int, encoding: String, workers: List[ActorContainer], dropFirsLines: Int, stopSystemAtTheEnd: Boolean, numberOfLinesPerMessage: Int, limitOfLinesRead: Option[Int]) extends Actor with Logging {
  val mBuffer = Source.fromFile(path, encoding)
  val mIterator = mBuffer.getLines().drop(dropFirsLines)
  val limitedmIterator =
    limitOfLinesRead
      .map(
        limit => mIterator
          .zipWithIndex
          .takeWhile {
          case (read, index) => index <= limit
          }
          .map {
          case (read, index) => read
      })
      .getOrElse(mIterator)
  val mSource = limitedmIterator.grouped(numberOfLinesPerMessage).zipWithIndex
  val mListWatchedRoutees = ArrayBuffer.empty[ActorRef]
  var bestSizes = List.fill(columnNumberExpected)(0)
  var operationFinished = false

  override def receive: Actor.Receive = {
    case Start() =>
      logger.debug(s"*** Watching ${sender().path} ***")
      workers.foreach {
        actor =>
          context.watch(actor.actor)
          mListWatchedRoutees += actor.actor
          if (actor.isRooter) actor.actor ! Broadcast(RegisterYourself()) // Will watch the rootees
      }
      self ! ReadNextBlock() // Start the process of reading the file
    case RegisterMe() =>
      logger.debug(s"*** Register rootee ${sender().path} ***")
      mListWatchedRoutees += sender
      context.watch(sender())
    case ReadNextBlock() =>
      logger.debug(s"*** Send lines ***")
      if (mSource.hasNext) {
        val (lines, index) = mSource.next()
        workers.foreach(_.actor ! Lines(lines, index))
      }
      else {
        if (!operationFinished) {
          logger.debug(s"*** Send poison pill to all workers ***")
          operationFinished = true
          workers.partition(_.isRooter) match {
            case (withRooter, withoutRooter) =>
              withRooter.foreach(_.actor ! Broadcast(PoisonPill))
              withoutRooter.foreach(_.actor ! PoisonPill)
          }
        }
      }
    case Terminated(ref) =>
      logger.debug(s"*** Rootee ${sender().path} is dead ***")
      mListWatchedRoutees -= ref
      if (mListWatchedRoutees.isEmpty) {
        logger.debug("*** Everybody is gone  ***")
        if (stopSystemAtTheEnd) context.system.shutdown()
      }
    case t =>
      throw new IllegalStateException(s"Bad parameter sent to ${self.path} ($t)")
  }

  override def postStop(): Unit = {
    mBuffer.close()
    super.postStop()
  }
}