package com.taj.supertaxlawyer.test

import org.scalatest.{ Outcome, fixture, Matchers, WordSpecLike }
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

/**
 * For each test, an Akka system is generated, therefore there is no chock in Akka names.
 */
trait TestTrait extends fixture.WordSpecLike with Matchers with Logging {

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