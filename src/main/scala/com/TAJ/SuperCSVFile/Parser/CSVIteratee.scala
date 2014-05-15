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

import scala.language.higherKinds
import scala.Some

object CSVIteratee extends App {

  import scalaz._, Scalaz._
  import iteratee._, Iteratee._
  import effect._
  import java.io.StringReader

  def takeWhile[A, F[_]](p: A ⇒ Boolean)(implicit mon: Monoid[F[A]], pt: Applicative[F]): Iteratee[A, F[A]] = {
      def loop(acc: F[A])(s: Input[A]): Iteratee[A, F[A]] =
        s(el = e ⇒
          if (p(e)) cont(loop(mon.append(acc, pt.point(e))))
          else done[A, Id, F[A]](acc, s), empty = cont(loop(acc)), eof = done[A, Id, F[A]](acc, eofInput)
        )
    cont(loop(mon.zero))
  }

  def take[A, F[_]](n: Int)(implicit mon: Monoid[F[A]], pt: Applicative[F]): Iteratee[A, F[A]] = {
      def loop(acc: F[A], n: Int)(s: Input[A]): Iteratee[A, F[A]] =
        s(el = e ⇒
          if (n <= 0) done[A, Id, F[A]](acc, s)
          else cont(loop(mon.append(acc, pt.point(e)), n - 1)), empty = cont(loop(acc, n)), eof = done[A, Id, F[A]](acc, s)
        )
    cont(loop(mon.zero, n))
  }

  def testest: IterateeT[String, IO, Seq[String]] = {
      def step(accumulator: Seq[String])(s: Input[String]): IterateeT[String, IO, Seq[String]] =
        s(el = e ⇒ if (accumulator.size == 2) done(accumulator :+ e, eofInput)
        else cont(step(accumulator :+ e)),
          empty = cont(step(accumulator)),
          eof = done(accumulator, eofInput)
        )
    cont(step(Seq()))
  }

  val i: Iterator[String] = io.Source.fromFile("./README.md").getLines()
  val e: EnumeratorT[String, IO] = enumIterator[String, IO](i)
  val t = ((testest &= e).run.unsafePerformIO())
  println(t)
  ((head[String, IO] &= e).run.unsafePerformIO()) assert_=== Some("## Super CSV File ##")

  val stream123 = enumStream[Int, Id](Stream(1, 2, 3))

  ((head[Int, Id] &= stream123).run) assert_=== Some(1)
  ((length[Int, Id] &= stream123).run) assert_=== 3
  ((peek[Int, Id] &= stream123).run) assert_=== Some(1)
  // ((head[Int, Id]   &= enumStream(Stream())).run) assert_=== None

  def iter123 = enumIterator[Int, IO](Iterator(1, 2, 3))

  ((head[Int, IO] &= iter123).run unsafePerformIO ()) assert_=== Some(1)
  ((length[Int, IO] &= iter123).run unsafePerformIO ()) assert_=== 3
  ((peek[Int, IO] &= iter123).run unsafePerformIO ()) assert_=== Some(1)
  ((head[Int, IO] &= enumIterator[Int, IO](Iterator())).run unsafePerformIO ()) assert_=== None

  val stream1_10 = enumStream[Int, Id]((1 to 10).toStream)

  (take[Int, List](3) &= stream1_10).run assert_=== List(1, 2, 3)
  (takeWhile[Int, List](_ <= 5) &= stream1_10).run assert_=== (1 to 5).toList
  (takeUntil[Int, List](_ > 5) &= stream1_10).run assert_=== (1 to 5).toList

  val readLn = takeWhile[Char, List](_ != '\n') flatMap (ln ⇒ drop[Char, Id](1).map(_ ⇒ ln))
  (collect[List[Char], List] %= readLn.sequenceI &= enumStream("Iteratees\nare\ncomposable".toStream)).run assert_=== List("Iteratees".toList, "are".toList, "composable".toList)

  def iter1234 = enumIterator[String, IO](Iterator("", "", ""))

  (collect[List[Int], List] %= splitOn(_ % 3 != 0) &= stream1_10).run assert_=== List(List(1, 2), List(4, 5), List(7, 8), List(10))

  (collect[Int, List] %= map((_: String).toInt) &= enumStream(Stream("1", "2", "3"))).run assert_=== List(1, 2, 3)
  (collect[Int, List] %= filter((_: Int) % 2 == 0) &= stream1_10).run assert_=== List(2, 4, 6, 8, 10)

  (collect[List[Int], List] %= group(3) &= enumStream((1 to 9).toStream)).run assert_=== List(List(1, 2, 3), List(4, 5, 6), List(7, 8, 9))

  def r: EnumeratorT[IoExceptionOr[Char], IO] = enumReader[IO](new StringReader("file contents"))

  ((head[IoExceptionOr[Char], IO] &= r).map(_ flatMap (_.toOption)).run.unsafePerformIO()) assert_=== Some('f')
  ((length[IoExceptionOr[Char], IO] &= r).run.unsafePerformIO()) assert_=== 13
  ((peek[IoExceptionOr[Char], IO] &= r).map(_ flatMap (_.toOption)).run.unsafePerformIO()) assert_=== Some('f')
  ((head[IoExceptionOr[Char], IO] &= enumReader[IO](new StringReader(""))).map(_ flatMap (_.toOption)).run unsafePerformIO ()) assert_=== None

  // As a monad
  val m1 = head[Int, Id] flatMap (b ⇒ head[Int, Id] map (b2 ⇒ (b tuple b2)))
  (m1 &= stream123).run assert_=== Some(1 -> 2)

  // As a monad using for comprehension (same as 'm1' example above)
  val m2 = for {
    b ← head[Int, Id]
    b2 ← head[Int, Id]
  } yield b tuple b2
  (m2 &= stream123).run assert_=== Some(1 -> 2)

  val colc = takeWhile[IoExceptionOr[Char], List](_.fold(_ ⇒ false, _ != ' ')).up[IO]
  ((colc &= r).map(_ flatMap (_.toOption)).run unsafePerformIO ()) assert_=== List('f', 'i', 'l', 'e')

  val take10And5ThenHead = take[Int, List](10) zip take[Int, List](5) flatMap (ab ⇒ head[Int, Id] map (h ⇒ (ab, h)))
  (take10And5ThenHead &= enumStream((1 to 20).toStream)).run assert_=== (((1 to 10).toList, (1 to 5).toList), Some(11))
}

