package com.taj.supertaxlawyer.test

import java.io.File


object DataToTest {
  val testResourcesFolder = s".${File.separator}src${File.separator}test${File.separator}resources${File.separator}"
  val encodedFileFolder = testResourcesFolder + s"encoded_files${File.separator}"
  val tempFilesFolder = testResourcesFolder + s"temp${File.separator}"

  val semicolon = TestContainer("semicolon.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), List(5, 7, 29, 5, 32, 4, 5, 4, 4, 4), ";", "ISO-8859-2", 25l)
  val pipe = TestContainer("pipe.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), List(5, 7, 29, 5, 32, 4, 5, 4, 4, 4), "|", "ISO-8859-2", 25l)
  val semicolon_with_title = TestContainer("semicolon_with_document_title_on_one_column.csv", 10, List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), List(7, 7, 29, 7, 32, 7, 7, 7, 7, 8), ";", "ISO-8859-2", 26l)
  val tab = TestContainer("tab.txt", 10, List(7, 7, 9, 7, 7, 7, 15, 7, 7, 20), List(4, 7, 9, 5, 4, 4, 15, 4, 4, 20), "\t", "ISO-8859-2", 24l)
  val fake_utf8 = TestContainer("utf8_file.txt", 3, List(24, 23, 27), List(24, 23, 23), ";", "ISO-8859-1", 2)
}
