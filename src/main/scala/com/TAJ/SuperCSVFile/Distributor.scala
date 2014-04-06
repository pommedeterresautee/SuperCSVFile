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

package com.TAJ.SuperCSVFile

import scala.io.Source
import akka.actor._
import com.TAJ.SuperCSVFile.ActorMessages.RequestMoreWork
import com.TAJ.SuperCSVFile.ActorMessages.Lines
import akka.routing.Broadcast
import java.io.{ FileInputStream, File }
import com.typesafe.scalalogging.slf4j.Logging
import java.util.concurrent.TimeUnit

case class ActorContainer(actor: ActorRef, isRooter: Boolean)

object Distributor {

  def apply(file: File, encoding: String, workers: List[ActorContainer], dropFirsLines: Option[Int], limitNumberOfLinesToRead: Option[Int])(implicit system: ActorSystem) = {
    val actor = new ComponentDistributor with DisplayProgress {
      override val distributor = system.actorOf(Props(new Distributor(file.getAbsolutePath, encoding, workers, dropFirsLines, numberOfLinesPerMessage = 500, limitNumberOfLinesToRead)), name = "Distributor")
    }

    actor.distributor
  }
}

object DistributorTest {

  def apply(file: File, encoding: String, workers: List[ActorContainer], dropFirsLines: Option[Int], numberOfLinesPerMessage: Int, limitNumberOfLinesToRead: Option[Int])(implicit system: ActorSystem) = {
    val actor = new ComponentDistributor with NoDisplayProgress {
      override val distributor = system.actorOf(Props(new Distributor(file.getAbsolutePath, encoding, workers, dropFirsLines, numberOfLinesPerMessage, limitNumberOfLinesToRead)), name = "DistributorTest")
    }

    actor.distributor
  }
}

trait Progress {
  def displayProgress(position: Long, fileSize: Long): Unit
  def newLine(): Unit
  def timeToReadFile(): Unit
}

trait DisplayProgress extends Progress {
  private case class TimeContainer(hours: Long, minutes: Long, seconds: Long)
  private var precedentPercentage = 0
  private val startTime = System.currentTimeMillis()

  override def displayProgress(position: Long, fileSize: Long) {
    val currentPercent = (position * 100 / fileSize).toInt
    if (currentPercent > precedentPercentage) {
      precedentPercentage = currentPercent
      val remaining = fileSize - position
      val timeUsed = System.currentTimeMillis() - startTime
      val timeRemaining = (remaining * timeUsed) / position
      val time = whatTimeIsIt(timeRemaining)
      print(s"\r[${"*" * (currentPercent / 5)}${" " * (20 - (currentPercent / 5))}] $currentPercent% (${timeString(time)}))")
    }
  }

  override def newLine() = print("\r")
  override def timeToReadFile() {
    val diff = System.currentTimeMillis() - startTime
    val time = whatTimeIsIt(diff)
    println(s"The file has been read in ${timeString(time)}")
  }

  private def timeString(time: TimeContainer): String = {
    s"${time.hours}h ${time.minutes}m ${time.seconds}s"
  }

  private def whatTimeIsIt(timeDiff: Long): TimeContainer = {
    var diff = timeDiff
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    diff = diff - (hours * 60 * 60 * 1000)
    val min = TimeUnit.MILLISECONDS.toMinutes(diff)
    diff = diff - (min * 60 * 1000)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    TimeContainer(hours, min, seconds)
  }
}

trait NoDisplayProgress extends Progress {
  override def displayProgress(position: Long, fileSize: Long) {}
  override def newLine() {}
  override def timeToReadFile() {}
}

trait ComponentDistributor {
  self: Progress ⇒

  val distributor: ActorRef

  /**
   * Read the file and send the work.
   * @param path path to the file to analyze.
   */
  class Distributor(path: String, encoding: String, workers: List[ActorContainer], dropFirsLines: Option[Int], numberOfLinesPerMessage: Int, limitOfLinesRead: Option[Int]) extends Actor with Logging {

    var thresholdJobWaiting = 200
    val fileSize = new File(path).length()
    val is = new FileInputStream(path)
    val channel = is.getChannel
    val mBuffer = Source.fromInputStream(is, encoding)
    val mIterator = mBuffer.getLines().drop {
      dropFirsLines match {
        case Some(linesToDrop) ⇒ linesToDrop
        case None              ⇒ 0
      }
    }
    val mLimitedIterator =
      limitOfLinesRead
        .map(
          limit ⇒ mIterator
            .zipWithIndex
            .takeWhile {
              case (read, index) ⇒ index <= limit
            }
            .map {
              case (read, index) ⇒ read
            })
        .getOrElse(mIterator)
    val mSource = mLimitedIterator.grouped(numberOfLinesPerMessage).zipWithIndex
    var operationFinished = false
    var notFinishedWork = 1 // count the number of actors which are awaiting for more work. Start with one because we send RequestMoreWork() in the start case (count for one).

    override def receive: Actor.Receive = {
      case RequestMoreWork() if notFinishedWork < thresholdJobWaiting ⇒
        notFinishedWork -= 1 // decrease by one the number of job done (current message).
        if (mSource.hasNext) {
          logger.debug(s"*** Send lines ***")
          val (lines, index) = mSource.next()
          workers.foreach {
            _.actor ! Lines(lines, index)
          }
          notFinishedWork += workers.size // increase the number of job sent.
          displayProgress(channel.position, fileSize)
        }
        else {
          if (!operationFinished && notFinishedWork == 0) { // 0 to wait that the list of Job is totally empty.
            logger.debug(s"*** Send poison pill to all workers ***")
            newLine()
            operationFinished = true
            workers.foreach {
              case ActorContainer(ref, true)  ⇒ ref ! Broadcast(PoisonPill)
              case ActorContainer(ref, false) ⇒ ref ! PoisonPill
            }
          }
        }
      case RequestMoreWork() ⇒
        notFinishedWork -= 1 // decrease by one the number of job done (current message).
        logger.debug(s"*** ${self.path.name} has received $notFinishedWork ${RequestMoreWork.getClass.getSimpleName} messages, the last is from ${sender().path.name} ***")
      case t ⇒
        throw new IllegalStateException(s"Bad parameter sent to ${self.path.name} ($t)")
    }

    override def postStop(): Unit = {
      mBuffer.close()
      channel.close()
      is.close()
      timeToReadFile()
    }
  }
}