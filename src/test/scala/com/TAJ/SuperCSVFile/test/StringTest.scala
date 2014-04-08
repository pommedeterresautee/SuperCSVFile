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
import com.TAJ.SuperCSVFile.FileStructure.FileTools

object StringTest extends TestTrait {
  val test: ((String, File, String)) ⇒ Unit = {
    case (name, file, encoding) ⇒
      s"We will evaluate the encoding of the file $name." should {
        s"The encoding should be detected as $encoding" in {
          val encoding = FileTools.detectEncoding(file.getAbsolutePath)
          encoding should equal(encoding)
        }
      }
  }

  def extractionOfEscapeCharacters() {
    "Test the extraction of escape characters from a String (\\Q.*\\E)" must {

      "Extraction of toto" in {
        val result = FileTools.removeEscapeChar("\\Qtoto\\E")
        result should be(Some("toto"))
      }

      "Extraction of |" in {
        val result = FileTools.removeEscapeChar("\\Q|\\E")
        result should be(Some("|"))
      }

      "Extraction of TAB char" in {
        val result = FileTools.removeEscapeChar("\\Q\t\\E")
        result should be(Some("\t"))
      }
    }
  }
}
