package com.taj.supercsvfile.test

import org.scalatest.Suites
import com.taj.supercsvfile.test.DataToTest._
import java.io.File

/**
 * These tests are related to the count of columns in a text file.
 */
class MainTest extends Suites(TestOnSizeColumnComparator, EncodingTest, ColumnSizeTests) with TestTrait {

  Seq(semicolon, semicolon_with_title, tab, pipe)
    .map(fileToTest ⇒ (fileToTest.name, new File(encodedFileFolder, fileToTest.name), fileToTest.encoding))
    .foreach(EncodingTest.test)

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
      .foreach(t ⇒ TestOnSizeColumnComparator.bestSize)
  }
}