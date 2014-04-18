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
import akka.routing.RoundRobinPool
import com.TAJ.SuperCSVFile.ActorMessages._

import com.TAJ.SuperCSVFile.ActorMessages.Lines
import scala.Some
import com.TAJ.SuperCSVFile.ActorContainer
import com.TAJ.SuperCSVFile.ActorMessages.RequestMoreWork
import com.typesafe.scalalogging.slf4j.Logging
import scalaz._
import Scalaz._
import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec
import com.TAJ.SuperCSVFile.Parser.OpenCSV

case class WrongLines(lines: Seq[(Int, String)])
case class ColumnSizes(lines: Seq[Int])

/**
 * Init a real size actor.
 */
object SizeActor {
  def apply(output: Option[String], expectedColumnQuantity: Int, splitter: Char, titles: Option[Seq[String]])(implicit system: ActorSystem): (ActorContainer, ActorRef, ActorRef) = {
    val routeesQuantity = Runtime.getRuntime.availableProcessors
    val actorTrait = new SizeActorTrait with AccumulatorSizeActorTrait {
      override val mColumnQuantity: Int = expectedColumnQuantity
      override val mSplitter: Char = splitter
      override val resultAccumulatorActor = system.actorOf(Props(new AccumulatorActor(routeesQuantity)), "AccumulatorActor")
      override val finalResultActor = system.actorOf(Props(new ResultSizeColumnActor(output, titles)), "ResultSizeColumnActor")
      override val sizeActor = system.actorOf(Props(new SizeActor(output)).withRouter(RoundRobinPool(routeesQuantity)), name = "SizeActor")
    }

    (ActorContainer(actorTrait.sizeActor, isRooter = true), actorTrait.resultAccumulatorActor, actorTrait.finalResultActor)
  }
}

/**
 * Init an actor to test the column size computation.
 */
object SizeActorInjectedResultActor {
  def apply(injectedFinalResultActor: ActorRef, expectedColumnQuantity: Int, splitter: Char)(implicit system: ActorSystem): (ActorContainer, ActorContainer) = {
    val routeesQuantity = Runtime.getRuntime.availableProcessors
    val actorTestTrait = new SizeActorTrait with AccumulatorSizeActorTrait {
      override val mColumnQuantity: Int = expectedColumnQuantity
      override val mSplitter: Char = splitter
      override val resultAccumulatorActor = system.actorOf(Props(new AccumulatorActor(routeesQuantity)), "TestAccumulatorActor")
      override val finalResultActor = injectedFinalResultActor
      override val sizeActor = system.actorOf(Props(new SizeActor(None)).withRouter(RoundRobinPool(Runtime.getRuntime.availableProcessors)), name = "TestSizeActor")
    }

    (ActorContainer(actorTestTrait.sizeActor, isRooter = true), ActorContainer(actorTestTrait.resultAccumulatorActor, isRooter = false))
  }
}

/**
 * Stores algorithms of column sizes computation.
 */
trait SizeComputation extends Logging {
  val mBiggestColumns: (Seq[Int], Seq[Int]) ⇒ Seq[Int] = (first, second) ⇒ first zip second map (tuple ⇒ tuple._1 max tuple._2)

  val mSplitter: Char
  val mColumnQuantity: Int
  lazy val parser = OpenCSV(delimiterChar = mSplitter)

  def mGetBestFitSize(listToAnalyze: Seq[String]): Seq[Int] = {
    val (correctSizeLines, _) =
      listToAnalyze
        .map(parser.parseLine)
        .map(_.getOrElse(Seq()))
        .zipWithIndex
        .partition { case (line, index) ⇒ line.size == mColumnQuantity }

    val correctLines =
      correctSizeLines
        .map { case (line, index) ⇒ line }
        .map(_.map(_.size).toSeq) // Change to a list of size of columns
        .foldLeft(Seq.fill(mColumnQuantity)(0))(mBiggestColumns) // Mix the best results
    correctLines
  }
}

trait SizeActorTrait extends SizeComputation {
  self: AccumulatorSizeActorTrait ⇒
  val sizeActor: ActorRef

  /**
   * Analyze each block of lines received and send back the best match of size of columns.
   */
  class SizeActor(output: Option[String]) extends Actor with Logging {
    var counter = 0

    override def receive: Actor.Receive = {
      case Lines(listToAnalyze, index) ⇒
        counter += listToAnalyze.length
        val blockResult: Seq[Int] =
          mGetBestFitSize(listToAnalyze)
        resultAccumulatorActor ! ColumnSizes(blockResult)
        //resultAccumulatorActor ! WrongLines(wrongLines)
        sender ! RequestMoreWork() // Ask for the next line
      case JobFinished() ⇒
        resultAccumulatorActor ! JobFinished()
        self ! PoisonPill
    }

    override def postStop(): Unit = {
      logger.debug(s"*** The actor ${self.path.name} has analyzed $counter lines ***")
    }
  }
}

trait AccumulatorSizeActorTrait extends SizeComputation {

  val resultAccumulatorActor: ActorRef
  val finalResultActor: ActorRef

  /**
   * Receive the result of each analyze, choose the best result and at the end of the process, display the result, or save it to a file.
   * @param workerQuantity Number of workers.
   */
  class AccumulatorActor(workerQuantity: Int) extends Actor with Logging {
    var bestSizes: Option[Seq[Int]] = None
    var workerFinished = 0
    val wrongSizeAccumulator: ArrayBuffer[(Int, String)] = ArrayBuffer()

    /**
     * Classify the lines by merging the one forming one line and the other non matching lines.
     * @param listToCheck lines to check.
     * @param size expected number of the columns.
     * @return a Tuple of good and bad lines)
     */
    def classifyLines(listToCheck: Seq[(Int, Seq[String])], size: Int): (Seq[Seq[String]], Seq[Seq[String]]) = {

        @tailrec
        def classifyLines(accumulateGood: Seq[Seq[String]], accumulateBad: Seq[Seq[String]], listToCheck: Seq[(Int, Seq[String])], size: Int): (Seq[Seq[String]], Seq[Seq[String]]) = {
          listToCheck match {
            case Nil                          ⇒ (accumulateGood, accumulateBad)
            case (indexHead, lineHead) :: Nil ⇒ (accumulateGood, accumulateBad :+ lineHead)
            case (indexHead, lineHead) :: (indexNext, lineNext) :: tail if indexHead + 1 == indexNext && lineHead.size + lineNext.size == size ⇒
              val newList: Seq[Seq[String]] = accumulateGood :+ (lineHead ++ lineNext)
              classifyLines(newList, accumulateBad, tail, size)
            case (indexHead, lineHead) :: (indexNext, lineNext) :: tail ⇒
              val newList = accumulateBad :+ lineHead
              classifyLines(accumulateGood, newList, (indexNext, lineNext) :: tail, size)
          }
        }

      classifyLines(Seq(), Seq(), listToCheck, size)
    }

    override def receive: Actor.Receive = {
      case ColumnSizes(columnSizes) ⇒
        bestSizes match {
          case None ⇒ bestSizes = Some(columnSizes)
          case Some(currentBestSize) ⇒
            bestSizes = mBiggestColumns(currentBestSize, columnSizes).some
        }
      case WrongLines(wrongSize) ⇒
      //        wrongSizeAccumulator ++= wrongSize
      case JobFinished() ⇒
        workerFinished += 1
        if (workerFinished == workerQuantity) {
          bestSizes.foreach(finalResultActor ! ColumnSizes(_))
          //          val sorted = wrongSizeAccumulator
          //            .sortBy { case (index, line) ⇒ index }
          self ! PoisonPill
        }
    }
  }
}

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
          However there is no match between the effective number of columns (${sizes.size}) and the expected number of columns (${titleList.size}) listed here after:
          ${titleList.mkString(";")}
          You should audit the result."""
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
