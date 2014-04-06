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