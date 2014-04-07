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

package com.TAJ.SuperCSVFile.Extractor

import akka.actor.{ PoisonPill, Props, ActorSystem, Actor }

import com.TAJ.SuperCSVFile.ActorContainer
import com.TAJ.SuperCSVFile.ActorMessages.{ JobFinished, RequestMoreWork, Lines }
import com.typesafe.scalalogging.slf4j.Logging
import scala.reflect.io.Path

object LineExtractorActor {
  def apply(output: Option[String])(implicit system: ActorSystem) = ActorContainer(system.actorOf(Props(new LineExtractorActor(output)), "ExtractLinesActor"), isRooter = false)
}

/**
 * Write the lines received in an output file.
 */
class LineExtractorActor(outputFile: Option[String]) extends Actor with Logging {

  import scala.reflect.io.File
  override def receive: Receive = {
    case Lines(lines, index) ⇒
      logger.debug(s"Received ${lines.size} lines.")
      outputFile match {
        case Some(filePath) if Path(filePath).isFile ⇒
          File(filePath).appendAll(lines.mkString("\n"))
        case Some(filePath) if !Path(filePath).isFile ⇒
          throw new IllegalArgumentException(s"Path provided is not a file: $filePath")
        case None ⇒ println(lines.mkString("\n"))
      }
      sender() ! RequestMoreWork()
    case JobFinished() ⇒
      self ! PoisonPill
  }

  override def postStop(): Unit = {
    logger.debug(s"*** Actor ${self.path.name} is dead. ***")
  }
}
