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
import akka.routing.RoundRobinRouter
import com.taj.supertaxlawyer.ActorMessages.RegisterMe
import com.taj.supertaxlawyer.ActorMessages.Lines
import com.taj.supertaxlawyer.ActorMessages.RegisterYourself
import scala.Some
import com.taj.supertaxlawyer.FileStructure.SizeActorMessages.Result
import com.taj.supertaxlawyer.ActorContainer
import com.taj.supertaxlawyer.ActorMessages.ReadNextBlock
import com.taj.supertaxlawyer.FileStructure.SizeActorMessages.Finished

trait ResultActorTrait {
  val resultActor: ActorRef

  // ordinal class
  /**
   * Receive the result of each analyze, choose the best result and at the end of the process, display the result, or save it to a file.
   * @param workerQuantity Number of workers.
   * @param output path to a file where to save the result.
   */
  class ResultSizeColumnActor(workerQuantity: Int, output: Option[String]) extends Actor {
    var bestSizes: Option[List[Int]] = None
    var workerFinished = 0

    override def receive: Actor.Receive = {
      case Result(columnSizes) =>
        bestSizes match {
          case None => bestSizes = Some(columnSizes)
          case Some(listReceived) => bestSizes = Some(SizeActor.mBiggerColumn(listReceived, columnSizes))
        }

      case Finished() =>
        workerFinished += 1
        if (workerFinished == workerQuantity) {
          val stringResult = bestSizes.get.mkString(";")

          output match {
            case Some(outputPath) => // a path to save the result in a file is provided
              import scala.reflect.io.File
              File(outputPath).writeAll(stringResult)
            case None => println(stringResult) // no path provided => display the result
          }

        }
    }
  }

}


object SizeActor {
  val mBiggerColumn: (List[Int], List[Int]) => List[Int] = (first, second) => first zip second map (tuple => tuple._1 max tuple._2)

  def apply(output: Option[String], expectedColumnQuantity: Int, splitter: String)(implicit system: ActorSystem): ActorContainer = {
    val rooteesQuantity = Runtime.getRuntime.availableProcessors

    class RealSizeActor
      extends SizeActorTrait
      with ResultActorTrait {
      val resultActor = system.actorOf(Props(new ResultSizeColumnActor(rooteesQuantity, output)), "ResultSizeColumnActor")
      val sizeActor = system.actorOf(Props(new SizeActor(output, expectedColumnQuantity, splitter)).withRouter(RoundRobinRouter(rooteesQuantity)), name = "SizeActor")
    }

    ActorContainer(new RealSizeActor().sizeActor, isRooter = true)
  }
}


trait SizeActorTrait {
  // specify dependencies by self-type annotation
  self: ResultActorTrait =>
  val sizeActor: ActorRef

  /**
   * Analyze each block of lines received and send back the best match of size of columns.
   * @param columnQuantity expected number of columns.
   * @param splitter char used to separate each column.
   */
  class SizeActor(output: Option[String], columnQuantity: Int, splitter: String) extends Actor {
    val emptyList = List.fill(columnQuantity)(0)

    override def receive: Actor.Receive = {
      case RegisterYourself() => sender ! RegisterMe()
      case Lines(listToAnalyze) =>
        val blockResult = listToAnalyze
          .map(_.split(splitter)) // transform the line in Array of columns
          .filter(_.size == columnQuantity) // Remove not expected lines
          .map(_.map(_.size).toList) // Change to a list of size of columns
          .foldLeft(emptyList)(SizeActor.mBiggerColumn) // Mix the best results
        resultActor ! Result(blockResult)
        sender ! ReadNextBlock() // Ask for the next line
    }

    override def postStop(): Unit = {
      resultActor ! Finished()
    }
  }


}


/**
 * Specific actor messages to the column size computer.
 */
private object SizeActorMessages {

  case class Result(columnSizes: List[Int])

  case class Finished()

}



