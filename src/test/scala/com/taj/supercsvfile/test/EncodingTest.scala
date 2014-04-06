package com.taj.supercsvfile.test

import java.io.File
import com.taj.supercsvfile.FileStructure.FileTools

object EncodingTest extends TestTrait {
  val test: ((String, File, String)) ⇒ Unit = {
    case (name, file, encoding) ⇒
      s"We will evaluate the encoding of the file $name." should {
        s"The encoding should be detected as $encoding" in {
          val encoding = FileTools.detectEncoding(file.getAbsolutePath)
          encoding should equal(encoding)
        }
      }
  }
}
