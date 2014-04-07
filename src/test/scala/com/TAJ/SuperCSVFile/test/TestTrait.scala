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

import org.scalatest._
import com.typesafe.scalalogging.slf4j.Logging
import akka.testkit.{ TestKitBase, ImplicitSender }
import akka.actor.ActorSystem
import java.util.concurrent.atomic.AtomicInteger

/**
 * Offer an atomic counter.
 */
private object TestSystemCounter {
  private val sysId = new AtomicInteger()
  def nextSysId() = sysId.incrementAndGet()
}

trait TestCommonTrait extends ShouldMatchers with Logging

trait TestTrait extends WordSpecLike with TestCommonTrait

/**
 * For each test, an Akka system is generated, therefore there is no chock in Akka names.
 */
trait TestTraitAkka extends fixture.WordSpecLike with TestCommonTrait {

  import TestSystemCounter._

  // ScalaTest needs to know what our Fixture parameter
  // type is
  type FixtureParam = AkkaFixture
  // Our basic Fixture. You would derive from this if you
  // want to specialize it with more fields, methods, etc...
  class AkkaFixture extends ImplicitSender with TestKitBase {
    override implicit lazy val system = ActorSystem(s"TestAkkaSystem${nextSysId()}")
  }
  // Abstract. Derivations must implement this
  def createAkkaFixture(): FixtureParam = new AkkaFixture
  // This is how our tests get run
  override def withFixture(test: OneArgTest) = {
    val sys = createAkkaFixture()
    test(sys)
  }
}