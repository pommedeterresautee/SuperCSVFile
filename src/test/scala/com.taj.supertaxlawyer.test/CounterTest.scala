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

package com.taj.supertaxlawyer.test

import org.scalatest._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import java.io.File
import akka.actor.ActorSystem
import com.taj.supertaxlawyer.FileStructure._
import com.taj.supertaxlawyer.Distributor
import com.taj.supertaxlawyer.ActorMessages.Start
import com.typesafe.scalalogging.slf4j.Logging


case class testContainer(name: String, numberOfColumns: Int, columnCountWithTitles: List[Int], columnCountWithoutTitles: List[Int], splitter: String, encoding: String, numberOfLines: Long)

/**
 * These tests are related to the count of columns in a text file.
 */
class CounterTest extends TestKit(ActorSystem("AkkaTestSystem")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with Logging {

  val testResourcesFolder = s".${File.separator}src${File.separator}test${File.separator}resources${File.separator}"
  val encodedFileFolder = testResourcesFolder + s"encoded_files${File.separator}"
  val tempFilesFolder = testResourcesFolder + s"temp${File.separator}"

  val semicolon = testContainer("semicolon.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), List(4, 7, 29, 5, 32, 4, 5, 4, 4, 4), ";", "ISO-8859-2", 25l)
  val semicolon_with_title = testContainer("semicolon_with_document_title_on_one_column.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), ";", "ISO-8859-2", 26l)
  val tab = testContainer("tab.txt", 10, List(7, 7, 9, 7, 7, 7, 15, 7, 7, 20), List(4, 7, 9, 5, 4, 4, 15, 4, 4, 20), "\t", "ISO-8859-2", 24l)

  /**
   * Clean all temp files before starting
   */
  override def beforeAll() {
    super.beforeAll()
  }

  Seq(semicolon, semicolon_with_title, tab)
    .foreach {
    fileToTest =>
      s"We will evaluate the column sizes of the file ${fileToTest.name}." must {
        val file = new File(encodedFileFolder, fileToTest.name)

        s"The encoding should be detected as ${fileToTest.encoding}" in {
          val encoding = FileSizeTools.detectEncoding(file.getAbsolutePath)
          encoding should equal(fileToTest.encoding)
        }

        s"The number of columns should be ${fileToTest.numberOfColumns}" in {
          FileSizeTools.columnCount(file.getAbsolutePath, fileToTest.splitter, fileToTest.encoding) should equal(fileToTest.numberOfColumns)
        }

        "The best size of columns including titles will be computed." in {
          val columnSizeTestActor: TestProbe = TestProbe()
          val linesTestActor: TestProbe = TestProbe()

          val testSizeActor = SizeActorTest(columnSizeTestActor, fileToTest.name, "first", fileToTest.numberOfColumns, fileToTest.splitter)

          val listOfWorkers = List(testSizeActor, LineCounterActorTest(linesTestActor, None))
          val distributor = Distributor(file, fileToTest.splitter, fileToTest.numberOfColumns, fileToTest.encoding, listOfWorkers, 0, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "first")
          distributor ! Start()

          columnSizeTestActor.expectMsg(fileToTest.columnCountWithTitles)
          linesTestActor.expectMsg(fileToTest.numberOfLines)
        }

        "The best size of columns without titles will be computed." in {
          val columnSizeTestActor: TestProbe = TestProbe()
          val linesTestActor: TestProbe = TestProbe()

          val testSizeActor = SizeActorTest(columnSizeTestActor, fileToTest.name, "bis", fileToTest.numberOfColumns, fileToTest.splitter)

          val listOfWorkers = List(testSizeActor, LineCounterActorTest(linesTestActor, None))
          val distributor = Distributor(file, fileToTest.splitter, fileToTest.numberOfColumns, fileToTest.encoding, listOfWorkers, 1, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "bis")
          distributor ! Start()

          columnSizeTestActor.expectMsg(fileToTest.columnCountWithoutTitles)
          linesTestActor.expectMsg(fileToTest.numberOfLines - 1) // -1 because we remove one line for the tittles
        }
      }
  }

  "We will compare 2 lists to find the best size." must {
    Seq((List(1, 2, 3, 1), List(4, 5, 3, 2), List(4, 5, 3, 2)), (List(1, 8, 3, 7), List(8, 9, 0, 5), List(8, 9, 3, 7)), (List(1, 2, -3, -1), List(-4, -5, 3, 2), List(1, 2, 3, 2)))
      .foreach {
      case (list1, list2, goodResult) =>
        s"Get the biggest list between $list1 and $list2." in {
          val result = CommonTools.mBiggerColumn(list1, list2)
          result should be(goodResult)
        }
    }
  }

  "We will compare several strings to find the best column size." must {
  Seq((List("first|second|third", "fourth|five|six", "seven|eight|nine"), "|", 3, List(6,6,5)),
    (List("one;second;fourth", "one;five;two", "one;eight;nineteen"), ";", 3, List(3,6,8)))
    .zipWithIndex
    .foreach{case ((listOfString, splitter, numberOfColumns, expectedResult), index )=>
      s"Size evaluation of the group $index." in {

        val result = CommonTools.mGetBestFitSize(listOfString, splitter, numberOfColumns, List.fill(numberOfColumns)(0))
        result should be (expectedResult)
      }
    }
  }

  /**
   * Stops all actors when tests are finished.
   * Delete all temp files.
   */
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    system.shutdown()
    super.afterAll()
  }
}