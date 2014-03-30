package com.taj.supertaxlawyer.test

import java.io.File
import com.taj.supertaxlawyer.FileStructure.FileSizeTools

object EncodingTest extends TestTrait {
  val test: ((String, File, String)) ⇒ Unit = {
    case (name, file, encoding) ⇒
      s"We will evaluate the encoding of the file $name." should {
        s"The encoding should be detected as $encoding" in {
          f ⇒
            val encoding = FileSizeTools.detectEncoding(file.getAbsolutePath)
            encoding should equal(encoding)
        }
      }
  }
}
