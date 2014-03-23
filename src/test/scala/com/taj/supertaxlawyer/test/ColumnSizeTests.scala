package com.taj.supertaxlawyer.test

import java.io.File
import com.taj.supertaxlawyer.FileStructure.{LineCounterActorTest, SizeActorTest, FileSizeTools}
import akka.testkit.{TestKit, TestKitBase, ImplicitSender, TestProbe}
import com.taj.supertaxlawyer.Distributor
import com.taj.supertaxlawyer.ActorMessages.Start
import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll


object ColumnSizeTests extends TestTrait with TestKitBase with ImplicitSender with BeforeAndAfterAll {

  implicit lazy val system = ActorSystem("AkkaTestSystem")

  val test: ((File, String, String, String, Long, Int, scala.List[Int], scala.List[Int])) => Unit = {
    case (file, name, encoding, splitter, numberOfLines, numberOfColumns, columnCountWithTitles, columnCountWithoutTitles) =>
      s"We will evaluate the column sizes of the file $name." must {

        val numberOfColumns = FileSizeTools.columnCount(file.getAbsolutePath, splitter, encoding)

        s"The number of columns should be $numberOfColumns" in {
          numberOfColumns should equal(numberOfColumns)
        }

        "The best size of columns including titles will be computed." in {
          val columnSizeTestActor: TestProbe = TestProbe()
          val linesTestActor: TestProbe = TestProbe()

          val testSizeActor = SizeActorTest(columnSizeTestActor, name, "first", numberOfColumns, splitter)

          val listOfWorkers = List(testSizeActor, LineCounterActorTest(linesTestActor, None))
          val distributor = Distributor(file, splitter, numberOfColumns, encoding, listOfWorkers, 0, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "first")
          distributor ! Start()

          columnSizeTestActor.expectMsg(columnCountWithTitles)
          linesTestActor.expectMsg(numberOfLines)
        }

        "The best size of columns without titles will be computed." in {
          val columnSizeTestActor: TestProbe = TestProbe()
          val linesTestActor: TestProbe = TestProbe()

          val testSizeActor = SizeActorTest(columnSizeTestActor, name, "bis", numberOfColumns, splitter)

          val listOfWorkers = List(testSizeActor, LineCounterActorTest(linesTestActor, None))
          val distributor = Distributor(file, splitter, numberOfColumns, encoding, listOfWorkers, 1, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "bis")
          distributor ! Start()

          columnSizeTestActor.expectMsg(columnCountWithoutTitles)
          linesTestActor.expectMsg(numberOfLines - 1) // -1 because we remove one line for the tittles
        }
      }
  }

  /**
   * Stops all actors when tests are finished.
   * Delete all temp files.
   */
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    system.shutdown()
  }
}
