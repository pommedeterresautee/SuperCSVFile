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
import akka.actor._
import com.taj.supertaxlawyer.CommonTools._
import scala.collection.mutable.ArrayBuffer
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import com.taj.supertaxlawyer.CommonTools.Register
import com.taj.supertaxlawyer.CommonTools.Lines
import com.taj.supertaxlawyer.CommonTools.RegisterYourself
import scala.Some
import akka.routing.Broadcast
import com.taj.supertaxlawyer.CommonTools.Result
import akka.actor.Terminated
import com.taj.supertaxlawyer.CommonTools.Start
import scala.concurrent.Await

private object CommonTools {
  val mBiggerColumn: (List[Int], List[Int]) => List[Int] = (theOld, theNew) => theOld.zip(theNew).map(t => t._1 max t._2)
  case class Lines(blockToAnalyze: Seq[String])
  case class Result(columnSizes: List[Int])
  case class Start()
  case class Register()
  case class RegisterYourself()

}

/**
 * Operation related to the count of columns in a text file.
 */
object ColumnSizeCounter {

  def compute(path: String, splitter: String, expectedColumnQuantity: Int, verbose:Boolean):List[Int] = {
    import akka.pattern.ask
    implicit val timeout = Timeout(2, TimeUnit.MINUTES)
    val numberOfLinesPerMessage = 300

    val system: ActorSystem = ActorSystem("ActorSystemColumnSizeComputation")
    val computer = system.actorOf(Props(new Distributor(path, splitter, expectedColumnQuantity, numberOfLinesPerMessage, verbose)), name = "DistributorWorker")
    val result = Await.result(computer ? Start(), timeout.duration) match {
      case Result(columnSizes) =>
        system.shutdown()
        columnSizes
      case t => throw new IllegalArgumentException("Failed to retrieve result from Actor during the check. " + t.toString)
    }

    result
  }
}

/**
 * Read the file and send the work.
 * @param path path to the file to analyze.
 * @param sizeMessage number of lines to send to each worker.
 * @param columnNumber expected number of columns.
 */
class Distributor(path: String, splitter:String, columnNumber: Int, sizeMessage: Int, verbose:Boolean) extends Actor {
  val mBuffer = Source.fromFile(path)
  val mSource = mBuffer.getLines().grouped(sizeMessage)
  val mListWatchedRoutees = ArrayBuffer.empty[ActorRef]
  var bestSizes = List.fill(columnNumber)(0)
  var mWorkerMaster: Option[ActorRef] = None
  var mOriginalSender: Option[ActorRef] = None
  var operationFinished = false

  override def receive: Actor.Receive = {
    case Start() =>
      if(verbose) println("*** Start treatment ***")
      mOriginalSender = Some(sender)
      mWorkerMaster = Some(context.actorOf(Props(new BlockAnalyzer(columnNumber, splitter)).withRouter(RoundRobinRouter(Runtime.getRuntime.availableProcessors)), name = "MasterBlockAnalyzer"))
      if(verbose) println(s"*** Watching ${sender.path} ***")
      context.watch(mWorkerMaster.get) // Watch the router
      mListWatchedRoutees += mWorkerMaster.get
      mWorkerMaster.get ! Broadcast(RegisterYourself()) // Will watch the rootees
    case Register() =>
      if(verbose) println(s"*** Register rootee ${sender.path} ***")
      mListWatchedRoutees += sender
      context.watch(sender)
    case Result(columnSizes) if columnSizes.size != columnNumber =>
      throw new IllegalStateException(s"Incorrect number of column: ${columnSizes.size} instead of $columnNumber")
    case Result(columnSizes) => bestSizes = mBiggerColumn(bestSizes, columnSizes)
      if(verbose) println(s"*** get result from ${sender.path} ***")
      if (mSource.hasNext) sender ! Lines(mSource.next())
      else {
        if(!operationFinished){
          if(verbose) println(s"*** Send poison pill to ${mWorkerMaster.get.path} ***")
          operationFinished = true
          mWorkerMaster.get ! Broadcast(PoisonPill)
        }
      }
    case Terminated(ref) =>
      if(verbose) println(s"*** Rootee ${sender.path} is dead ***")
      mListWatchedRoutees -= ref
      if(verbose) println(s"*** There are still ${mListWatchedRoutees.size} rootees alive ***")
      if (mListWatchedRoutees.isEmpty) {
        mOriginalSender.get ! Result(bestSizes)
      }
    case t =>
      throw new IllegalStateException(s"Bad parameter sent to ${self.path} ($t)")
  }


  override def postStop(): Unit = {
    mBuffer.close()
    super.postStop()
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
        .map(_.split(splitter)) // transform the line in Array of columns
        .filter(_.size == columnNumber) // Remove not expected lines
        .map(_.map(_.size).toList) // Change to a list of size of columns
        .foldLeft(emptyList)(mBiggerColumn) // Mix the best results
      sender ! Result(blockResult)
    case RegisterYourself() => sender ! Register()
      sender ! Result(emptyList) // to start the process: send a fake empty result 
    //case _ => throw new IllegalStateException(s"Bad parameter sent to ${self.path}")
  }
}


