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

import java.io.FileInputStream
import scala.io.Source
import akka.actor.{Terminated, PoisonPill, Actor, ActorRef}
import com.taj.supertaxlawyer.CommonTools._
import scala.collection.mutable.ArrayBuffer
import akka.routing.Broadcast

private object CommonTools {
  val mBiggerColumn: (List[Int], List[Int]) => List[Int] = (theOld, theNew) => theOld.zip(theNew).map(t => t._1 max t._2)

  case class Lines(blockToAnalyze: Seq[String])

  case class Result(columnSizes: List[Int])

  case class Start(worker: ActorRef)

  case class Register(worker: ActorRef)

  case class RegisterYourself()

}

/**
 * Operation related to the count of columns in a text file.
 */
class ColumnSizeCounter {


  private def distributor(path: String, sizeMessage: Int, actor: ActorRef) = {
    Source.fromFile(path).getLines().grouped(sizeMessage).next()
  }


  /**
   * Give the position of the last char of the line after moving to the tartPosition.
   * @param path path to the file to work on.
   * @param startPosition position where to begin the search.
   * @return the position of the last char of the line.
   */
  def lastCharOnLine(path:String, startPosition:Long):Long = {
    val is = new FileInputStream(path)
    is.skip(startPosition)

    val position = Iterator.continually(is.read()).map(_.toChar).indexWhere(c => c == '\n' || c == '\r')

    is.close()
    startPosition + position
  }


}

/**
 * Read the file and send the work.
 * @param path path to the file to analyze.
 * @param sizeMessage number of lines to send to each worker.
 * @param columnNumber expected number of columns.
 */
class Distributor(path: String, sizeMessage: Int, columnNumber: Int) extends Actor {

  val mSource = Source.fromFile(path).getLines().grouped(sizeMessage)
  var bestSizes = List.fill(columnNumber)(0)
  var master: Option[ActorRef] = None
  val listOfRoutee = ArrayBuffer.empty[ActorRef]

  override def receive: Actor.Receive = {
    case Start(actor) =>
      actor ! Broadcast(RegisterYourself())
      master = Some(sender)
    case Result(columnSizes) if columnSizes.size != columnNumber =>
      throw new IllegalStateException(s"Incorrect number of column: ${columnSizes.size} instead of $columnNumber")
    case Result(columnSizes) => bestSizes = mBiggerColumn(bestSizes, columnSizes)
      if (mSource.hasNext) sender ! Lines(mSource.next())
      else sender ! Broadcast(PoisonPill)
    case Register(routee) =>
      listOfRoutee += routee
      context.watch(routee)
    case Terminated(ref) =>
      println(s"*** has been removed ${ref.path} ***")
      listOfRoutee -= ref
      if (listOfRoutee.isEmpty) {
        master.get ! listOfRoutee
        context.stop(self)
      }
    case t =>
      throw new IllegalStateException(s"Bad parameter sent to ${self.path} ($t)")
  }
}

/**
 * Analyze each block of lines received and send back the best match of size of columns.
 * @param columnNumber expected number of columns.
 * @param splitter char used to separate each column.
 */
class BlockAnalyzer(columnNumber: Int, splitter: String) extends Actor {
  val emptyList = List.fill(columnNumber)(0)

  override def receive: Actor.Receive = {
    case Lines(listToAnalyze) =>
      val blockResult = listToAnalyze
        .map(_.split(splitter))
        .filter(_.size == columnNumber) // Remove not expected lines
        .map(_.map(_.size).toList) // Change to a list of size of columns
        .foldLeft(emptyList)(mBiggerColumn) // Mix the best results
      sender ! Result(blockResult)
    case RegisterYourself() => sender ! Register(self)
      sender ! Result(emptyList) // to start the process: send a fake empty result 
    case t => throw new IllegalStateException(s"Bad parameter sent to ${self.path} ($t)")
  }
}


