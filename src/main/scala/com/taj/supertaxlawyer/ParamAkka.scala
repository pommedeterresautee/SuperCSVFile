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

package com.taj.supertaxlawyer

object ParamAkka {

  val bufferSize: Int = 1024

  /**
   * Size of each part sent to each Actor.
   * Speed test for different parameters based on a 400 Mb file (time in ms).
   * Size   Time
   * 2   Kb 151 021
   * 1   Mb  25 205
   * 10  Mb  22 207
   * 20  Mb  21 384 <- Best
   * 70  Mb  22 691
   * 100 Mb  23 671
   */
  val sizeOfaPartToAnalyze = 1024 * 1024 * 20

  /**
   * Compute the number of AKKA workers needed to process the file.
   * Computation based on the size of the file and the size of the segments to analyze.
   * If we are working on a small file, start less workers, if it s a big file, use the number of processor cores.
   * @param fileSize size of the file to process
   * @return the number of workers.
   */
  def numberOfWorkerRequired(fileSize: Long) =
    (1 to Runtime.getRuntime.availableProcessors)
      .find(_ * sizeOfaPartToAnalyze >= fileSize)
      .getOrElse(Runtime.getRuntime.availableProcessors)
}