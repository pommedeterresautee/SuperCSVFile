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

import akka.actor.ActorRef

object ActorMessages {

  case class Lines(blockToAnalyze: Seq[String], blockIndex: Int)

  case class Start()

  case class RequestMoreWork()

  /**
   * Contains positions of fields listed in the Article A 47 A-1 of the Book of tax procedures.
   * @param journalCode Journal ID.
   * @param journalName Journal name.
   * @param entryNbr Entry line ID.
   * @param entryDate Entry date.
   * @param accountNbr Account number used.
   * @param accountLabel Account name.
   * @param auxAccountNbr Auxiliary account number.
   * @param auxAccountLabel Auxiliary account name.
   * @param label Label of the entry.
   * @param debit debit amount.
   * @param credit credit amount.
   * @param clearanceCode Code used for clearance.
   * @param clearanceDate Date of the clearance.
   * @param validDate Date of validation.
   * @param currencyValue Currency value of the entry.
   * @param currencyName Currency name of the entry.
   */
  case class EntryFieldPositions(journalCode: Option[Int], journalName: Option[Int], entryNbr: Option[Int], entryDate: Option[Int], accountNbr: Option[Int], accountLabel: Option[Int], auxAccountNbr: Option[Int], auxAccountLabel: Option[Int], label: Option[Int], debit: Option[Int], credit: Option[Int], clearanceCode: Option[Int], clearanceDate: Option[Int], validDate: Option[Int], currencyValue: Option[Int], currencyName: Option[Int])

  case class AccountEntry(accountNbr: Int, accountLabel: String, debit: Long, credit: Long) {
    def this(line: Seq[String], positions: EntryFieldPositions) = this(
      line(positions.accountNbr.get).toInt,
      line(positions.accountLabel.get),
      line(positions.debit.get).toLong,
      line(positions.credit.get).toLong
    )
  }

  case class TestToApply(actor: ActorRef, rooter: Boolean)

  case class JobFinished()

}
