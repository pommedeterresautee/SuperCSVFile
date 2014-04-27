package com.TAJ.SuperCSVFile.test

import com.TAJ.SuperCSVFile.Parser.{ ParserIterator, OpenCSV }

object IteratorParser extends TestTrait {
  def test(): Unit = {
    "Test the iterator parser with" should {
      "with a correctly built multiline entry" in {

        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka"
                        |
                        |ckdnsklgfasg;fnsdkjagf""".stripMargin.split('\n').toIterator

        val expected = List(List("test", "test2"), List("""seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka""".stripMargin), List(""), List("ckdnsklgfasg", "fnsdkjagf"))

        val parser = OpenCSV(DelimiterChar = ';')
        val par = ParserIterator(parser, toParse)
        val result = par.toList
        result shouldBe expected
      }
      "with a not correctly built multiline entry" in {

        val toParse = """test;test2
                        |"seconde ligne
                        |troisieme ligne
                        |quatrieme ligne;test3
                        |encore;deux;etTrois
                        |fmklsgnal;fnghka
                        |
                        |ckdnsklgfasg;fnsdkjagf""".stripMargin.split('\n').toIterator

        val expected = List(List("test", "test2"),
          List("""seconde ligne
                  |troisieme ligne
                  |quatrieme ligne;test3
                  |encore;deux;etTrois
                  |fmklsgnal;fnghka
                  |
                  |ckdnsklgfasg;fnsdkjagf""".stripMargin))

        val parser = OpenCSV(DelimiterChar = ';')
        val par = ParserIterator(parser, toParse)
        val result = par.toList
        result shouldBe expected
      }
    }
  }
}
