package com.taj.supertaxlawyer.test

import com.taj.supertaxlawyer.FileStructure.SizeComputation

object TestOnSizeColumnComparator extends TestTrait with SizeComputation {
  val biggestList: ((List[Int], List[Int], List[Int])) ⇒ Unit = {
    case (list1, list2, goodResult) ⇒
      s"Get the biggest list between $list1 and $list2." in {
        val result = mBiggestColumns(list1, list2)
        result shouldBe goodResult
      }
  }

  val bestSize: ((List[String], String, Int, List[Int]), Int) ⇒ Unit = {
    case (((listOfString, splitter, numberOfColumns, expectedResult), index)) ⇒
      s"Size evaluation of the group $index." in {

        val result = mGetBestFitSize(listOfString, splitter, numberOfColumns, List.fill(numberOfColumns)(0))
        result shouldBe expectedResult
      }
  }
}
