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
import akka.routing.Broadcast
import com.taj.supertaxlawyer.ActorMessages._


case class ActorContainer(actor: ActorRef, isRooter: Boolean)

/**
 * Read the file and send the work.
 * @param path path to the file to analyze.
 * @param sizeMessage number of lines to send to each worker.
 * @param columnNumber expected number of columns.
 */
class Distributor(path: String, splitter: String, columnNumber: Int, sizeMessage: Int, codec: Codec, workers: List[ActorContainer], verbose: Boolean) extends Actor {
  val mBuffer = Source.fromFile(path)(codec)
  val mSource = mBuffer.getLines().grouped(sizeMessage)
  val mListWatchedRoutees = ArrayBuffer.empty[ActorRef]
  var bestSizes = List.fill(columnNumber)(0)
  var operationFinished = false

  override def receive: Actor.Receive = {
    case Start() =>
      if (verbose) println(s"*** Watching ${sender.path} ***")
      workers.foreach {
        actor =>
          context.watch(actor.actor)
          mListWatchedRoutees += actor.actor
          if (actor.isRooter) actor.actor ! Broadcast(RegisterYourself()) // Will watch the rootees
      }
      self ! NextBlock()
    case RegisterMe() =>
    if (verbose) println(s"*** Register rootee ${sender.path} ***")
      mListWatchedRoutees += sender
      context.watch(sender)
    case NextBlock() =>
      if (verbose) println(s"*** Send lines ***")
      if (mSource.hasNext) {
        workers.filter(!_.isRooter).foreach(_.actor ! Lines(mSource.next()))
        workers.filter(_.isRooter).foreach(_.actor ! Broadcast(Lines(mSource.next())))
      }
      else {
        if (!operationFinished) {
          if (verbose) println(s"*** Send poison pill to all workers ***")
          operationFinished = true
          workers.filter(_.isRooter).map(_.actor).foreach(_ ! Broadcast(PoisonPill))
          workers.filter(!_.isRooter).map(_.actor).foreach(_ ! PoisonPill)
        }
      }
    case Terminated(ref) =>
      if (verbose) println(s"*** Rootee ${sender.path} is dead ***")
      mListWatchedRoutees -= ref
      if (mListWatchedRoutees.isEmpty) {
        if (verbose) println("*** Everybody is gone  ***")
        context.system.shutdown()
      }
    case t =>
      throw new IllegalStateException(s"Bad parameter sent to ${self.path} ($t)")
  }

  override def postStop(): Unit = {
    mBuffer.close()
    super.postStop()
  }
}