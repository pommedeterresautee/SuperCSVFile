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

package com.taj.supertaxlawyer.Accounting

import akka.actor.{ Props, ActorSystem, Actor }
import com.taj.supertaxlawyer.ActorMessages._
import com.taj.supertaxlawyer.ActorMessages.RegisterMe
import com.taj.supertaxlawyer.ActorMessages.Lines
import com.taj.supertaxlawyer.ActorMessages.RegisterYourself
import com.taj.supertaxlawyer.ActorMessages.ReadNextBlock
import com.taj.supertaxlawyer.ActorContainer

/**
 * Messages between Actors.
 */
object ExtractEntry {
  def apply(positions: EntryFieldPositions, splitter: String)(implicit system: ActorSystem): ActorContainer = ActorContainer(system.actorOf(Props(new ExtractEntry(positions, splitter)), name = "ExtractEntry"), isRooter = false)
}

/**
 * Extract entry from a provided String.
 */
class ExtractEntry(positions: EntryFieldPositions, splitter: String) extends Actor {
  override def receive: Actor.Receive = {
    case RegisterYourself() ⇒ sender ! RegisterMe()
    case Lines(lines, index) ⇒
      val entry: Seq[AccountEntry] = lines.map(_.split(splitter)).map(new AccountEntry(_, positions))
      println(s"received ${entry.size}")
      sender ! ReadNextBlock() // Ask for the next line
  }
}