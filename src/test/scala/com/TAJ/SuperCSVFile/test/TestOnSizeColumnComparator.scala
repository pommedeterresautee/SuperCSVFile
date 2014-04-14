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

import com.TAJ.SuperCSVFile.FileStructure.SizeComputation

object TestOnSizeColumnComparator extends TestTrait with SizeComputation {
  val biggestList: ((List[Int], List[Int], List[Int])) ⇒ Unit = {
    case (list1, list2, goodResult) ⇒
      s"Get the biggest list between $list1 and $list2." in {
        val result = mBiggestColumns(list1, list2)
        result shouldBe goodResult
      }
  }

  val bestSize: ((List[String], Char, Int, List[Int]), Int) ⇒ Unit = {
    case (((listOfString, splitter, numberOfColumns, expectedResult), index)) ⇒
      s"Size evaluation of the group $index." in {
        val result = mGetBestFitSize(listOfString, splitter, numberOfColumns, List.fill(numberOfColumns)(0))
        result shouldBe expectedResult
      }
  }
}
