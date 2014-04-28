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

import com.TAJ.SuperCSVFile.Parser.{ ParserIterator, OpenCSV }
import scala.collection.mutable.ArrayBuffer

object ParserIteratorTest extends TestTrait {
  def test(): Unit = {
    val eol = System.getProperty("line.separator")
    "Test the iterator parser with" should {
      "with a correctly built multiline entry" in {

        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka"
                        |
                        |ckdnsklgfasg;fnsdkjagf""".stripMargin.split(eol).toIterator

        val expected = List(List("test", "test2"), List("""seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka""".stripMargin), List(""), List("ckdnsklgfasg", "fnsdkjagf"))

        val parser = OpenCSV(DelimiterChar = ';')
        val par = ParserIterator(parser, toParse, None)
        val result = par.toList
        result shouldBe expected
      }

      "with a not correctly built multiline entry without back parsing" in {

        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka
                        |
                        |ckdnsklgfasg;fnsdkjagf""".stripMargin.split(eol).toIterator

        val expected = List(List("test", "test2"), List("seconde ligne"), List("troisieme ligne"), List("quatrieme ligne", "test3"), List("encore", "deux", "etTrois"), List("fmklsgnal", "fnghka"), List(""), List("ckdnsklgfasg", "fnsdkjagf"))

        val parser = OpenCSV(DelimiterChar = ';')
        val par = ParserIterator(parser, toParse, None)
        val result = par.toList
        result shouldBe expected
      }

      "with a not correctly built multiline entry and a limit in back parsing not reached" in {
        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne"
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka
                        |
                        |ckdnsklgfasg;fnsdkjagf""".stripMargin.split(eol).toIterator

        val parser = OpenCSV(DelimiterChar = ';')
        val par = ParserIterator(parser, toParse, Some(1))
        val result = par.toList

        val expected = List(List("test", "test2"), ArrayBuffer("""seconde ligne
          |troisieme ligne""".stripMargin), List("quatrieme ligne", "test3"), List("encore", "deux", "etTrois"), List("fmklsgnal", "fnghka"), List(""), List("ckdnsklgfasg", "fnsdkjagf"))

        result shouldBe expected
      }

      "with a correctly built multiline entry and a limit in back parsing which is reached" in {
        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3"
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka
                        |
                        |ckdnsklgfasg;fnsdkjagf""".stripMargin.split(eol).toIterator

        val parser = OpenCSV(DelimiterChar = ';')
        val par = ParserIterator(parser, toParse, Some(1))
        val result = par.toList

        val expected = List(List("test", "test2"), ArrayBuffer("seconde ligne"), List("troisieme ligne"), ArrayBuffer("test3"), List("encore", "deux", "etTrois"), List("fmklsgnal", "fnghka"), List(""), List("ckdnsklgfasg", "fnsdkjagf"))

        result shouldBe expected
      }
    }
  }
}