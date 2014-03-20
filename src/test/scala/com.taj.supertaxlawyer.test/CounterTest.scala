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


case class testContainer(name: String, numberOfColumns: Int, columnCount: List[Int], splitter: String, encoding: String, numberOfLines: Long)

/**
 * These tests are related to the count of columns in a text file.
 */
class CounterTest extends TestKit(ActorSystem("AkkaSystemForTest")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  val testResourcesFolder = s".${File.separator}src${File.separator}test${File.separator}resources${File.separator}"
  val encodedFileFolder = testResourcesFolder + s"encoded_files${File.separator}"
  val tempFilesFolder = testResourcesFolder + s"temp${File.separator}"

  val semicolon = testContainer("semicolon.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), ";", "ISO-8859-2", 25l)
  val semicolon_with_title = testContainer("semicolon_with_document_title_on_one_column.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), ";", "ISO-8859-2", 26l)
  val tab = testContainer("tab.txt", 10, List(7, 7, 9, 7, 7, 7, 15, 7, 7, 20), "\t", "ISO-8859-2", 24l)

  /**
   * Clean all temp files before starting
   */
  override def beforeAll() {
    super.beforeAll()
  }

  Seq(semicolon, semicolon_with_title, tab)
    .foreach {
    fileToTest =>
      s"${fileToTest.name} related to the column size count" must {
        val file = new File(encodedFileFolder, fileToTest.name)

        s"The encoding will be detected as ${fileToTest.encoding}" in {
          val encoding = FileSizeTools.detectEncoding(file.getAbsolutePath)
          encoding should equal(fileToTest.encoding)
        }

        s"The number of columns should be ${fileToTest.numberOfColumns}" in {
          FileSizeTools.columnCount(file.getAbsolutePath, fileToTest.splitter, fileToTest.encoding) should equal(fileToTest.numberOfColumns)
        }

        "The best size of columns will be determined" in {
          val columnSizeTestActor: TestProbe = TestProbe()
          val linesTestActor: TestProbe = TestProbe()

          val testSizeActor = SizeActorTest(columnSizeTestActor, fileToTest.name, fileToTest.numberOfColumns, fileToTest.splitter)

          val listOfWorkers = List(testSizeActor, LineCounterActorTest(linesTestActor, None))
          val distributor = Distributor(file, fileToTest.splitter, fileToTest.numberOfColumns, fileToTest.encoding, listOfWorkers, 0, stopSystemAtTheEnd = false)
          distributor ! Start()

          columnSizeTestActor.expectMsg(fileToTest.columnCount)
          linesTestActor.expectMsg(fileToTest.numberOfLines)
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