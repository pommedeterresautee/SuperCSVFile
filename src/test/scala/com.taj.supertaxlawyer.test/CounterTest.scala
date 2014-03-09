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
import com.taj.supertaxlawyer.FileStructure.{LineCounterActor, SizeActor, FileSizeTools}
import com.taj.supertaxlawyer.Distributor
import com.taj.supertaxlawyer.ActorMessages.Start


case class testContainer(name: String, numberOfColumns: Int, columnCount: List[Int], splitter: String, encoding: String)

/**
 * These tests are related to the count of columns in a text file.
 */
class CounterTest extends TestKit(ActorSystem("AkkaSystemForTest")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  val testResourcesFolder = s".${File.separator}src${File.separator}test${File.separator}resources${File.separator}"
  val encodedFileFolder = testResourcesFolder + s"encoded_files${File.separator}"
  val tempFilesFolder = testResourcesFolder + s"temp${File.separator}"

  val semicolon = testContainer("semicolon.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), ";", "ISO-8859-2")
  val semicolon_with_title = testContainer("semicolon_with_document_title_on_one_column.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), ";", "ISO-8859-2")
  val tab = testContainer("tab.txt", 10, List(7, 7, 9, 7, 7, 7, 15, 7, 7, 20), "\t", "ISO-8859-2")

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

        "The encoding will be detected" in {
          val encoding = FileSizeTools.detectEncoding(file.getAbsolutePath)
          encoding should equal(fileToTest.encoding)
        }

        "The number of columns is computed" in {
          FileSizeTools.columnCount(file.getAbsolutePath, fileToTest.splitter, fileToTest.encoding) should equal(fileToTest.numberOfColumns)
        }

        "The best size of columns will be determined" in {
          val myTestActor: TestProbe = TestProbe()

          val listOfWorkers = List(SizeActor(None, fileToTest.numberOfColumns,
            fileToTest.splitter, Some(myTestActor, file.getName)),
            LineCounterActor(None))
          val distributor = Distributor(file, fileToTest.splitter, fileToTest.numberOfColumns, fileToTest.encoding, listOfWorkers, verbose = false, stopSystemAtTheEnd = false)
          distributor ! Start()

          myTestActor.expectMsg(fileToTest.columnCount)
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