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

import com.TAJ.SuperCSVFile.Parser.ParserIterator
import scala.collection.mutable.ArrayBuffer
import com.TAJ.SuperCSVFile.Parser.ParserTypes._

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

        val expected = List(
          SuccessParser(ArrayBuffer("test", "test2"), 0, 0),
          SuccessParser(ArrayBuffer("""seconde ligne
          |troisieme ligne
          |quatrieme ligne;test3
          |encore;deux;etTrois
          |fmklsgnal;fnghka""".stripMargin), 1, 5),
          SuccessParser(ArrayBuffer(""), 6, 6),
          SuccessParser(ArrayBuffer("ckdnsklgfasg", "fnsdkjagf"), 7, 7))

        val par = ParserIterator(DelimiterChar = ';', IteratorOfLines = toParse, BackParseLimit = None)
        val result = par.toList
        result shouldBe expected
      }

      "with a not correctly built multi-line entry without back parsing" in {

        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka
                        |
                        |ckdnsklgfasg;fnsdkjagf"""
          .stripMargin
          .split(eol)
          .toIterator

        val expected = List(
          SuccessParser(ArrayBuffer("test", "test2"), 0, 0),
          FailedParser(List("seconde ligne"), """"seconde ligne""", 1),
          SuccessParser(ArrayBuffer("troisieme ligne"), 2, 2),
          SuccessParser(ArrayBuffer("quatrieme ligne", "test3"), 3, 3),
          SuccessParser(ArrayBuffer("encore", "deux", "etTrois"), 4, 4),
          SuccessParser(ArrayBuffer("fmklsgnal", "fnghka"), 5, 5),
          SuccessParser(ArrayBuffer(""), 6, 6),
          SuccessParser(ArrayBuffer("ckdnsklgfasg", "fnsdkjagf"), 7, 7))

        val par = ParserIterator(DelimiterChar = ';', IteratorOfLines = toParse, BackParseLimit = None)
        val result = par.toList

        result shouldBe expected
      }

      "with a correctly built multiline entry without limit in back parsing" in {
        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne"
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka
                        |
                        |ckdnsklgfasg;fnsdkjagf"""
          .stripMargin
          .split(eol)
          .toIterator

        val par = ParserIterator(DelimiterChar = ';', IteratorOfLines = toParse, BackParseLimit = None)
        val result = par.toList

        val expected = List(
          SuccessParser(ArrayBuffer("test", "test2"), 0, 0),
          SuccessParser(ArrayBuffer("""seconde ligne
          |troisieme ligne""".stripMargin), 1, 2),
          SuccessParser(ArrayBuffer("quatrieme ligne", "test3"), 3, 3),
          SuccessParser(ArrayBuffer("encore", "deux", "etTrois"), 4, 4),
          SuccessParser(ArrayBuffer("fmklsgnal", "fnghka"), 5, 5),
          SuccessParser(ArrayBuffer(""), 6, 6),
          SuccessParser(ArrayBuffer("ckdnsklgfasg", "fnsdkjagf"), 7, 7))

        result shouldBe expected
      }

      "with a correctly built multi-line entry and a limit in back parsing which is not reached." in {
        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka
                        |"
                        |ckdnsklgfasg;fnsdkjagf"""
          .stripMargin
          .split(eol)
          .toIterator

        val par = ParserIterator(DelimiterChar = ';', IteratorOfLines = toParse, BackParseLimit = Some(2))
        val result = par.toList
        val expected = List(SuccessParser(ArrayBuffer("test", "test2"), 0, 0),
          FailedParser(List("seconde ligne"), """"seconde ligne""", 1),
          SuccessParser(ArrayBuffer("troisieme ligne"), 2, 2),
          SuccessParser(ArrayBuffer("quatrieme ligne", "test3"), 3, 3),
          SuccessParser(ArrayBuffer("encore", "deux", "etTrois"), 4, 4),
          SuccessParser(ArrayBuffer("fmklsgnal", "fnghka"), 5, 5),
          FailedParser(List(""), "\"", 6),
          SuccessParser(ArrayBuffer("ckdnsklgfasg", "fnsdkjagf"), 7, 7))

        result shouldBe expected
      }

      "with one quote in a field and a limit in back parsing which is reached" in {
        val toParse = """test;test2
                        |seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test"3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka
                        |
                        |ckdnsklgfasg;fnsdkjagf""".stripMargin.split(eol).toIterator

        val par = ParserIterator(DelimiterChar = ';', IteratorOfLines = toParse, BackParseLimit = Some(3))
        val result = par.toList
        val expected = List(
          SuccessParser(ArrayBuffer("test", "test2"), 0, 0),
          SuccessParser(ArrayBuffer("seconde ligne"), 1, 1),
          SuccessParser(ArrayBuffer("troisieme ligne"), 2, 2),
          FailedParser(List("quatrieme ligne", "test3"), "quatrieme ligne;test\"3", 3),
          SuccessParser(ArrayBuffer("encore", "deux", "etTrois"), 4, 4),
          SuccessParser(ArrayBuffer("fmklsgnal", "fnghka"), 5, 5),
          SuccessParser(ArrayBuffer(""), 6, 6),
          SuccessParser(ArrayBuffer("ckdnsklgfasg", "fnsdkjagf"), 7, 7))

        result shouldBe expected
      }

      "with one quote in a single line" in {
        val toParse = Seq("test;tes\"t2;test3").toIterator

        val par = ParserIterator(DelimiterChar = ';', IteratorOfLines = toParse, BackParseLimit = Some(3))
        val result = par.toList
        val expected =
          List(FailedParser(List("test", "test2;test3"), "test;tes\"t2;test3", 0))

        result shouldBe expected
      }

      "with two quotes in two lines" in {
        val toParse = Seq("un,d\"eux", "trois\",quatre", "cinq,six").toIterator

        val par = ParserIterator(DelimiterChar = ',', IteratorOfLines = toParse, BackParseLimit = Some(3))
        val result = par.toList
        val expected =
          List(SuccessParser(ArrayBuffer("""deux
                |trois""".stripMargin, "quatre"), 0, 1), SuccessParser(ArrayBuffer("cinq", "six"), 2, 2))

        result shouldBe expected
      }

      "with two quotes in row on one line" in {
        val toParse = Seq("un,\"\"deux,trois").toIterator

        val par = ParserIterator(DelimiterChar = ',', IteratorOfLines = toParse, BackParseLimit = Some(3))
        val result = par.toList
        val expected =
          List(SuccessParser(List("un", "\"deux", "trois"), 0, 0))

        result shouldBe expected
      }
    }
  }
}