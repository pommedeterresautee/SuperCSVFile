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

import java.io.FileInputStream

/**
 * Operation related to the count of columns in a text file.
 */
class ColumnSizeCounter {

  /**
   * Give the position of the last char of the line after moving to the tartPosition.
   * @param path path to the file to work on.
   * @param startPosition position where to begin the search.
   * @return the position of the last char of the line.
   */
  def lastCharOnLine(path:String, startPosition:Long):Long = {
    val is = new FileInputStream(path)
    is.skip(startPosition)

    val position = Iterator.continually(is.read()).map(_.toChar).indexWhere(c => c == '\n' || c == '\r')

    is.close()
    startPosition + position
  }


}
