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

package com.TAJ.SuperCSVFile.test

import java.io.File
import com.TAJ.SuperCSVFile.FileStructure.{ LineCounterActorTest, SizeActorInjectedResultActor, FileTools }
import akka.testkit.TestProbe
import com.TAJ.SuperCSVFile.DistributorTest
import org.scalatest.BeforeAndAfterAll
import scalaz._
import Scalaz._
import scala.concurrent.duration._
import scala.concurrent.duration
import scala.Some
import com.TAJ.SuperCSVFile.FileStructure.ColumnSizes
import com.TAJ.SuperCSVFile.ActorMessages.RequestMoreWork
import com.TAJ.SuperCSVFile.Parser.OpenCSV

object ColumnSizeTests extends TestTraitAkka with BeforeAndAfterAll {

  val test: ((File, String, String, Char, Long, Int, scala.List[Int], scala.List[Int])) ⇒ Unit = {
    case (file, name, encoding, splitter, numberOfLines, numberOfColumns, columnCountWithTitles, columnCountWithoutTitles) ⇒

      s"We will evaluate the number of columns in the file $name." must {
        s"METHOD 1 - The number of columns should be $numberOfColumns" in {
          f ⇒
            val result = FileTools.columnCount(file.getAbsolutePath, splitter, encoding)

            result should be(numberOfColumns)
        }

        s"METHOD 2 - The number of columns should be $numberOfColumns and the splitter should be [$splitter]." in {
          f ⇒
            val (resultSplitter, resultColumns) = FileTools.findColumnDelimiter(file.getAbsolutePath, encoding)

            resultColumns should be(numberOfColumns)
            resultSplitter should be(splitter)
        }
      }

      s"We will test the OpenCSV parser for the file $name." must {
        val lines = io.Source.fromFile(file, encoding).getLines().toSeq
        lines.size shouldBe numberOfLines // check we have correctly read the file. +1 because we remove the first one (may contain a title)
        val parser = OpenCSV(delimiterChar = splitter)
        val parsedLines = lines.map(parser.parseLine(_).getOrElse(Seq()))
        withClue(
          s"""File $name has failed, it should have $numberOfColumns columns.
          |${parsedLines.map(_.size).mkString(";")}""".stripMargin) {
            parsedLines.drop(1).forall(_.size == numberOfColumns) shouldBe true
          }
      }

      s"We will evaluate the best column sizes of the file $name." must {

        "The best size of columns including titles will be computed." in {
          f ⇒
            implicit val system = f.system
            val columnSizeTestProbe: TestProbe = TestProbe()
            val columnSizeTestActor = columnSizeTestProbe.ref
            val (testSizeActor, _) = SizeActorInjectedResultActor(columnSizeTestActor, numberOfColumns, splitter)

            val listOfWorkers = List(testSizeActor)
            val distributor = DistributorTest(file, encoding, listOfWorkers, numberOfLinesPerMessage = 2, dropFirsLines = None, limitNumberOfLinesToRead = None)
            distributor ! RequestMoreWork()
            import duration._
            val time = FiniteDuration(10000, SECONDS)
            columnSizeTestProbe.expectMsg(time, ColumnSizes(columnCountWithTitles))
        }

        "The number of lines including titles will be computed." in {
          f ⇒
            implicit val system = f.system
            val linesTestActor: TestProbe = TestProbe()
            val listOfWorkers = List(LineCounterActorTest(linesTestActor.ref, None))
            val distributor = DistributorTest(file, encoding, listOfWorkers, numberOfLinesPerMessage = 2, dropFirsLines = None, limitNumberOfLinesToRead = None)
            distributor ! RequestMoreWork()
            val time = FiniteDuration(10000, SECONDS)
            linesTestActor.expectMsg(time, numberOfLines)
        }

        "The best size of columns without titles will be computed." in {
          f ⇒
            implicit val system = f.system
            val columnSizeTestProbe: TestProbe = TestProbe()
            val columnSizeTestActor = columnSizeTestProbe.ref
            val (testSizeActor, _) = SizeActorInjectedResultActor(columnSizeTestActor, numberOfColumns, splitter)
            val listOfWorkers = List(testSizeActor)
            val distributor = DistributorTest(file, encoding, listOfWorkers, dropFirsLines = Some(1), numberOfLinesPerMessage = 2, limitNumberOfLinesToRead = None)
            distributor ! RequestMoreWork()

            columnSizeTestProbe.expectMsg(ColumnSizes(columnCountWithoutTitles))
        }

        "The number of lines excluding titles will be computed." in {
          f ⇒
            implicit val system = f.system
            val linesTestActor: TestProbe = TestProbe()
            val listOfWorkers = List(LineCounterActorTest(linesTestActor.ref, None))
            val distributor = DistributorTest(file, encoding, listOfWorkers, dropFirsLines = 1.some, numberOfLinesPerMessage = 2, limitNumberOfLinesToRead = None)
            distributor ! RequestMoreWork()
            // -1 because we remove one line for the tittles
            linesTestActor.expectMsg(numberOfLines - 1)
        }

        "We will compute the number of lines read when we begin at line 2 (included) and finish at line 6" in {
          f ⇒
            implicit val system = f.system
            val linesTestActor: TestProbe = TestProbe()
            val listOfWorkers = List(LineCounterActorTest(linesTestActor.ref, None))
            val distributor = DistributorTest(file, encoding, listOfWorkers, dropFirsLines = 1.some, numberOfLinesPerMessage = 2, limitNumberOfLinesToRead = 5.some)
            distributor ! RequestMoreWork()
            // use min in case the file is too small.
            linesTestActor.expectMsg((numberOfLines - 1) min 6)
        }
      }
  }
}
