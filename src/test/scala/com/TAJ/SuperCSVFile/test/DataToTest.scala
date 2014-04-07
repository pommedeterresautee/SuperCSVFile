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

case class TestContainer(name: String, numberOfColumns: Int, columnCountWithTitles: List[Int], columnCountWithoutTitles: List[Int], splitter: String, encoding: String, numberOfLines: Long)

object DataToTest {
  val testResourcesFolder = s".${File.separator}src${File.separator}test${File.separator}resources${File.separator}"
  val encodedFileFolder = testResourcesFolder + s"encoded_files${File.separator}"
  val tempFilesFolder = testResourcesFolder + s"temp${File.separator}"

  val semicolon = TestContainer("semicolon.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), List(5, 7, 29, 5, 32, 4, 5, 4, 4, 4), ";", "ISO-8859-2", 25l)
  val pipe = TestContainer("pipe.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), List(5, 7, 29, 5, 32, 4, 5, 4, 4, 4), "|", "ISO-8859-2", 25l)
  val semicolon_with_title = TestContainer("semicolon_with_document_title_on_one_column.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), ";", "ISO-8859-2", 26l)
  val tab = TestContainer("tab.txt", 10, List(7, 7, 9, 7, 7, 7, 15, 7, 7, 20), List(4, 7, 9, 5, 4, 4, 15, 4, 4, 20), "\t", "ISO-8859-2", 24l)
  val utf8 = TestContainer("utf8_file.txt", 16, List(3, 16, 10, 8, 10, 50, 10, 20, 14, 8, 12, 9, 9, 11, 8, 8), List(3, 16, 10, 8, 10, 50, 1, 1, 14, 8, 12, 9, 9, 11, 8, 8), "\t", "UTF-8", 6)
  val fake_utf8 = TestContainer("utf8_file_fake.txt", 3, List(24, 23, 27), List(24, 23, 23), ";", "ISO-8859-1", 3)
}