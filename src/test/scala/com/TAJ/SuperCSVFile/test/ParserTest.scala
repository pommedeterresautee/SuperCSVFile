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

import com.TAJ.SuperCSVFile.Parser.OpenCSV
import scalaz._

object ParserTest extends TestTrait {
  def test(): Unit = {
    "We will evaluate OpenCSV parser" should {
      "Test parser with a simple line." in {
        val simpleLine = """test1;test2;test3"""
        val parsed = OpenCSV(delimiterChar = ';').parseLine(simpleLine)
        parsed shouldBe Success(Seq("test1", "test2", "test3"))
      }
      "Test parser with a quoted line." in {
        val quotedLine = """test1;test2;"test3;test3""""
        val parsedWithQuotes = OpenCSV(delimiterChar = ';').parseLine(quotedLine)
        parsedWithQuotes shouldBe Success(Seq("test1", "test2", "test3;test3"))
      }
      "Test parser with a double quoted." in {
        val quotedLine = """test1;test2;te""st4"""
        val parsedWithQuotes = OpenCSV(delimiterChar = ';').parseLine(quotedLine)
        parsedWithQuotes shouldBe Success(Seq("test1", "test2", "te\"st4"))
      }
      "Test parser with a double quoted in quoted field." in {
        val quotedLine = """test1;test2;"test3;te""st3""""
        val parsedWithQuotes = OpenCSV(delimiterChar = ';').parseLine(quotedLine)
        parsedWithQuotes shouldBe Success(Seq("test1", "test2", "test3;te\"st3"))
      }
      "Test parser with a quoted line and a break." in {
        val quotedLine =
          """test1;test2;"test3;
            |test3"""".stripMargin
        val parsedWithQuotes = OpenCSV(delimiterChar = ';').parseLine(quotedLine)
        parsedWithQuotes.getOrElse(Seq()) zip List("test1", "test2",
          """test3;
            |test3""".stripMargin) foreach {
            case (parsed, solution) ⇒ parsed shouldBe solution
          }
      }
      "Test parser with a wrongly constructed sequence." in {
        val quotedLine = "test1;test2;\"test3;test3"
        val parsedWithQuotes = OpenCSV(delimiterChar = ';').parseLine(quotedLine)
        parsedWithQuotes shouldBe Failure(Seq("test1", "test2", "test3;test3"))
      }
      "Test parser with three quotes sequence." in {
        val quotedLine = "test1;test2;\"\"\"test3;test3\"\"\""
        val parsedWithQuotes = OpenCSV(delimiterChar = ';').parseLine(quotedLine)
        parsedWithQuotes shouldBe Success(Seq("test1", "test2", "\"test3;test3\""))
      }
      "Test with a complex sequence." in {
        val text = Seq("John,Doe,120 jefferson st.,Riverside, NJ, 08076",
          "Jack,McGinnis,220 hobo Av.,Phila, PA,09119",
          "\"John \"\"Da Man\"\"\",Repici,120 Jefferson St.,Riverside, NJ,08075",
          "Stephen,Tyler,\"7452 Terrace \"\"At the Plaza\"\" road\",SomeTown,SD, 91234,Blankman,,SomeTown, SD, 00298",
          "\"Joan \"\"the bone\"\", Anne\",Jet,\"9th, at Terrace plc\",Desert City,CO,00123")

        val result = Seq(
          Seq("John", "Doe", "120 jefferson st.", "Riverside", " NJ", " 08076"),
          Seq("Jack", "McGinnis", "220 hobo Av.", "Phila", " PA", "09119"),
          Seq("John \"Da Man\"", "Repici", "120 Jefferson St.", "Riverside", " NJ", "08075"),
          Seq("Stephen", "Tyler", "7452 Terrace \"At the Plaza\" road", "SomeTown", "SD", " 91234"),
          Seq("Blankman", "", "SomeTown", " SD", " 00298"),
          Seq("Joan \"the bone\", Anne", "Jet", "9th, at Terrace plc", "Desert City", "CO", "00123")
        ).flatMap(l ⇒ l)

        val parsed = text.flatMap(OpenCSV(delimiterChar = ',').parseLine(_).getOrElse(Seq("Failllllllluuuuuuurrrrreeeeeee")))
        withClue(s"The parsed list is:\n${parsed.mkString(";")} instead of:\n${result.mkString(";")}") {
          parsed zip result foreach {
            case (computed, solution) ⇒
              computed shouldBe solution
          }
        }
      }
    }
  }
}