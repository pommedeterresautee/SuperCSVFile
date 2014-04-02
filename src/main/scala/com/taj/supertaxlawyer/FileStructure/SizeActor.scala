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

package com.taj.supertaxlawyer.FileStructure

import akka.actor._
import akka.routing.RoundRobinPool
import com.taj.supertaxlawyer.ActorMessages._

import akka.testkit.TestProbe
import com.taj.supertaxlawyer.ActorMessages.Lines
import scala.Some
import com.taj.supertaxlawyer.ActorContainer
import com.taj.supertaxlawyer.ActorMessages.RequestMoreWork
import com.typesafe.scalalogging.slf4j.Logging
import scalaz._
import Scalaz._
import scala.collection.mutable.ArrayBuffer

case class WrongLines(lines: Seq[String])
case class ColumnSizes(lines: Seq[Int])

/**
 * Init a real size actor.
 */
object SizeActor {
  def apply(output: Option[String], expectedColumnQuantity: Int, splitter: String, titles: Option[Seq[String]])(implicit system: ActorSystem): (ActorContainer, ActorRef, ActorRef) = {
    val rooteesQuantity = Runtime.getRuntime.availableProcessors

    val actorTrait = new SizeActorTrait with AccumulatorSizeActorTrait with ResultSizeActorTrait {
      override val resultAccumulatorActor = system.actorOf(Props(new AccumulatorActor(rooteesQuantity)), "AccumulatorActor")

      override val resultActor = system.actorOf(Props(new ResultSizeColumnActor(output, titles)), "ResultSizeColumnActor")

      val sizeActor = system.actorOf(Props(new SizeActor(output, expectedColumnQuantity, splitter)).withRouter(RoundRobinPool(rooteesQuantity)), name = "SizeActor")
    }
    (ActorContainer(actorTrait.sizeActor, isRooter = true), actorTrait.resultAccumulatorActor, actorTrait.resultActor)
  }
}

/**
 * Init an actor to test the column size computation.
 */
object SizeActorTest {

  def apply(testActor: TestProbe, expectedColumnQuantity: Int, splitter: String)(implicit system: ActorSystem): ActorContainer = {

    val routeesQuantity = Runtime.getRuntime.availableProcessors

    val actorTestTrait = new SizeActorTrait with AccumulatorSizeActorTrait with ResultSizeActorTrait {
      override val resultAccumulatorActor = system.actorOf(Props(new AccumulatorActor(routeesQuantity)), "TestAccumulatorActor")

      override val resultActor = testActor.ref

      val sizeActor = system.actorOf(Props(new SizeActor(None, expectedColumnQuantity, splitter)).withRouter(RoundRobinPool(Runtime.getRuntime.availableProcessors)), name = "TestSizeActor")
    }
    ActorContainer(actorTestTrait.sizeActor, isRooter = true)
  }
}

/**
 * Stores algorithms of column sizes computation.
 */
trait SizeComputation extends Logging {
  val mBiggestColumns: (Seq[Int], Seq[Int]) ⇒ Seq[Int] = (first, second) ⇒ first zip second map (tuple ⇒ tuple._1 max tuple._2)

  def mGetBestFitSize(listToAnalyze: Seq[String], splitter: String, columnQuantity: Int, emptyList: Seq[Int]): (Seq[Int], Seq[String]) = {
    if (emptyList.size != columnQuantity) throw new IllegalArgumentException(s"Empty list size is ${emptyList.size} and column quantity provided is $columnQuantity")
    val escapeRegexSplitter = s"\\Q$splitter\\E"
    val (correctSizeLines, notCorrectSizeLines) =
      listToAnalyze
        .map(_.split(escapeRegexSplitter, -1)) // transform the line in Array of columns
        .partition(line ⇒ line.size == columnQuantity)

    val result = correctSizeLines.filter(_.size == columnQuantity) // Remove not expected lines
      .map(_.map(_.size).toSeq) // Change to a list of size of columns
      .foldLeft(emptyList)(mBiggestColumns) // Mix the best results
    (result, notCorrectSizeLines.map(_.mkString(escapeRegexSplitter)))
  }
}

trait SizeActorTrait extends SizeComputation {
  self: AccumulatorSizeActorTrait ⇒

  /**
   * Analyze each block of lines received and send back the best match of size of columns.
   * @param columnQuantity expected number of columns.
   * @param splitter char used to separate each column.
   */
  class SizeActor(output: Option[String], columnQuantity: Int, splitter: String) extends Actor with Logging {
    val emptyList = List.fill(columnQuantity)(0)
    var counter = 0

    override def receive: Actor.Receive = {
      case Lines(listToAnalyze, index) ⇒
        counter += listToAnalyze.length

        val (blockResult: Seq[Int], wrongLines: Seq[String]) =
          mGetBestFitSize(listToAnalyze, splitter, columnQuantity, emptyList)
        resultAccumulatorActor ! ColumnSizes(blockResult)
        //        resultAccumulatorActor ! WrongLines(wrongLines)
        sender ! RequestMoreWork() // Ask for the next line
    }

    override def postStop(): Unit = {
      resultAccumulatorActor ! JobFinished()
      logger.debug(s"*** The actor ${self.path.name} has analyzed $counter lines ***")
    }
  }
}

trait AccumulatorSizeActorTrait extends SizeComputation {
  self: ResultSizeActorTrait ⇒

  val resultAccumulatorActor: ActorRef

  /**
   * Receive the result of each analyze, choose the best result and at the end of the process, display the result, or save it to a file.
   * @param workerQuantity Number of workers.
   */
  class AccumulatorActor(workerQuantity: Int) extends Actor with Logging {
    var bestSizes: Option[Seq[Int]] = None
    var workerFinished = 0
    val wrongSizeAccumulator: ArrayBuffer[String] = ArrayBuffer()

    override def receive: Actor.Receive = {
      case ColumnSizes(columnSizes) ⇒
        bestSizes match {
          case None ⇒ bestSizes = Some(columnSizes)
          case Some(currentBestSize) ⇒
            bestSizes = mBiggestColumns(currentBestSize, columnSizes).some
        }

      case WrongLines(wrongSize) ⇒
        wrongSizeAccumulator ++= wrongSize

      case JobFinished() ⇒
        workerFinished += 1
        if (workerFinished == workerQuantity) {
          bestSizes.foreach(resultActor ! ColumnSizes(_))
          self ! PoisonPill
        }
    }
  }
}

trait ResultSizeActorTrait {
  val resultActor: ActorRef

  class ResultSizeColumnActor(outputFile: Option[String], titles: Option[Seq[String]]) extends Actor with Logging {

    override def receive: Actor.Receive = {
      case ColumnSizes(bestSizes) ⇒
        val stringResult: String = (bestSizes, titles) match {
          case (sizes, Some(titleList)) if sizes.size == titleList.size ⇒
            titleList
              .zip(sizes)
              .map {
                case (title, size) ⇒ s"$title;$size"
              }
              .mkString("\n")
          case (sizes, Some(titleList)) if sizes.size != titleList.size ⇒
            s"""The program has found a result equal to:
            ${sizes.mkString(";")}
            However there is no match between the effective number of columns (${sizes.size}) and the expected number of columns ${titleList.size}.
            You should audit the result.
            """"
          case (sizes, None) ⇒ sizes.mkString(";")
        }

        outputFile match {
          case Some(outputPath) ⇒ // a path to save the result in a file is provided
            import scala.reflect.io._
            if (Path(outputPath).isFile) File(outputPath).writeAll(stringResult)
            else throw new IllegalArgumentException(s"Path provided is not to a file: $outputPath.")
          case None ⇒ println(stringResult)
        }
        self ! PoisonPill
    }
  }
}