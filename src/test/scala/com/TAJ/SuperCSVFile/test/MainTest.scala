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

import org.scalatest.Suites
import com.TAJ.SuperCSVFile.test.DataToTest._
import java.io.File

/**
 * These tests are related to the count of columns in a text file.
 */
class MainTest extends Suites(TestOnSizeColumnComparator, StringTest, ColumnSizeTests, ParserTest, IteratorParser) with TestTrait {

  Seq(semicolon, semicolon_with_title, tab, pipe)
    .map(fileToTest ⇒ (fileToTest.name, new File(encodedFileFolder, fileToTest.name), fileToTest.encoding))
    .foreach(StringTest.test)

  Seq(semicolon, semicolon_with_title, tab, pipe, utf8, fake_utf8)
    .map(fileToTest ⇒
      (new File(encodedFileFolder, fileToTest.name),
        fileToTest.name,
        fileToTest.encoding,
        fileToTest.splitter,
        fileToTest.numberOfLines,
        fileToTest.numberOfColumns,
        fileToTest.columnCountWithTitles,
        fileToTest.columnCountWithoutTitles
      ))
    .foreach(ColumnSizeTests.test)

  "We will compare 2 lists to find the best size." must {
    Seq((List(1, 2, 3, 1), List(4, 5, 3, 2), List(4, 5, 3, 2)), (List(1, 8, 3, 7), List(8, 9, 0, 5), List(8, 9, 3, 7)), (List(1, 2, -3, -1), List(-4, -5, 3, 2), List(1, 2, 3, 2)))
      .foreach(TestOnSizeColumnComparator.biggestList)
  }

  "We will compare several strings to find the best column size." must {
    Seq((List("first|second|third", "fourth|five|six", "seven|eight|nine"), "|", 3, List(6, 6, 5)),
      (List("one;second;fourth", "one;five;two", "one;eight;nineteen"), ";", 3, List(3, 6, 8)))
      .zipWithIndex
      .foreach(_ ⇒ TestOnSizeColumnComparator.bestSize)
  }

  StringTest.extractionOfEscapeCharacters()

  ParserTest.test()

  IteratorParser.test()
}