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
import akka.testkit.{ImplicitSender, TestKit}
import java.io.{FileInputStream, RandomAccessFile, File}
import akka.actor.ActorSystem
import com.taj.supertaxlawyer.{ColumnSizeCounter, ParamAkka}


case class testContainer(path: String, columnCount: Int, splitter:String)

/**
 * These tests are related to the count of columns in a text file.
 */
class CounterTest extends TestKit(ActorSystem("AkkaSystemForTest")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  val testResourcesFolder = s".${File.separator}src${File.separator}test${File.separator}resources${File.separator}"
  val encodedFileFolder = testResourcesFolder + s"encoded_files${File.separator}"
  val tempFilesFolder = testResourcesFolder + s"temp${File.separator}"

  val semicolon = testContainer("semicolon.csv", 10, ";")
  val tab = testContainer("tab.csv", 10, ";")

  /**
   * Clean all temp files before starting
   */
  override def beforeAll() {
    super.beforeAll()
  }

  Seq(semicolon, tab)
    .foreach {
    fileToTest =>
      val file = new File(encodedFileFolder, fileToTest.path)

      s"" must {
        val result = ColumnSizeCounter.compute(file.getAbsolutePath, fileToTest.splitter, fileToTest.columnCount, verbose = false)
        println("result=" + result.toString())
      }
  }

  /**
   * Stops all actors when tests are finished.
   * Delete all temp files.
   */
  override def afterAll(): Unit = {
    system.shutdown()
    super.afterAll()
  }
}
