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

package com.TAJ.SuperCSVFile.Parser

//import scala.language.higherKinds

import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits._
import scala.util._
import com.TAJ.SuperCSVFile.Parser.ParserTypes.ParserResult

object CSVIteratee extends App {

  // Defines the Iteratee[Int, Int]
  def total2Chunks: Iteratee[Int, Int] = {
      // `step` function is the consuming function receiving previous context (idx, total) and current chunk
      // context : (idx, total) idx is the index to count loops
      def step(idx: Int, total: Int)(i: Input[Int]): Iteratee[Int, Int] = i match {
        // chunk is EOF or Empty => simply stops iteration by triggering state Done with current total

        // found one chunk
        case Input.El(e) ⇒
          // if first or 2nd chunk, call `step` again by incrementing idx and computing new total
          if (idx < 2) Cont[Int, Int](int ⇒ step(idx + 1, total + e)(int))
          // if reached 2nd chunk, stop iterating
          else {
            println("tmp result: " + total)
            Done(total, i)
          }
        case Input.Empty ⇒ Done(total, Input.Empty)
        case Input.EOF   ⇒ Done(total, Input.EOF)
      }
    // initiates iteration by initialize context and first state (Cont) and launching iteration
    Cont[Int, Int](i ⇒ step(0, 0)(i))
  }

  //val enumeratorMe: Enumerator[String] = Enumerator.enumerate(scala.io.Source.fromFile("myfile.txt").getLines())

  val t = Enumeratee.mapConcat[Seq[Int]](identity)

  val enumerator = Enumerator(Seq(1, 234, 5, 7, 9), Seq(455, 9, 5, 3, 9, 7)) &> t

  val temp = t &>> total2Chunks

  val sum: Enumeratee[Int, Int] = Enumeratee.grouped(total2Chunks)

  val list: Enumerator[Int] = enumerator &> sum

  val iterator: Iteratee[Int, Int] = Iteratee.fold[Int, Int](0) { (total, elt) ⇒
    val e = total + elt
    println("total = " + e)
    e
  }

  //  val total = list |>>> iterator
  //
  //  total.onComplete {
  //    case Success(figure) ⇒ println(figure)
  //    case Failure(ex) ⇒
  //      println(s"They broke their promises! Again! Because of a ${ex.getMessage}")
  //  }

  val enumeratore = Enumerator("first line,toujours first,encore", "seconde,tralala,toto", "heyhey,hoho")

  val iter = ParserEnumeratee().parserIteratee()

  val group: Enumeratee[String, ParserResult] = Enumeratee.grouped(iter)

  val count = Iteratee.fold[ParserResult, Int](0) { (total, elt) ⇒
    val e = total + 1
    println("current = " + elt)
    e
  }

  val something = enumeratore &> group |>>> count

  something.onComplete {
    case Success(figure) ⇒ println(figure)
    case Failure(ex) ⇒
      println(s"They broke their promises! Again! Because of a ${ex.getMessage}")
  }

  Thread.sleep(10000)
}