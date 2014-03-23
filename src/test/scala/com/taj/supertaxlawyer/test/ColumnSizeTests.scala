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
          val testSizeActor = SizeActorTest(columnSizeTestActor, name, "first", numberOfColumns, splitter)

          val listOfWorkers = List(testSizeActor)
          val distributor = Distributor(file, splitter, numberOfColumns, encoding, listOfWorkers, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "first")
          distributor ! Start()

          columnSizeTestActor.expectMsg(columnCountWithTitles)

        }

        "The number of lines including titles will be computed." in {
          val linesTestActor: TestProbe = TestProbe()
          val listOfWorkers = List(LineCounterActorTest(linesTestActor, None))
          val distributor = Distributor(file, splitter, numberOfColumns, encoding, listOfWorkers, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "lineCounterWithTitles")
          distributor ! Start()

          linesTestActor.expectMsg(numberOfLines)
        }

        "The best size of columns without titles will be computed." in {
          val columnSizeTestActor: TestProbe = TestProbe()
          val testSizeActor = SizeActorTest(columnSizeTestActor, name, "bis", numberOfColumns, splitter)
          val listOfWorkers = List(testSizeActor)
          val distributor = Distributor(file, splitter, numberOfColumns, encoding, listOfWorkers, dropFirsLines = 1, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "bis")
          distributor ! Start()
          columnSizeTestActor.expectMsg(columnCountWithoutTitles)
        }


        "The number of lines excluding titles will be computed." in {
          val linesTestActor: TestProbe = TestProbe()
          val listOfWorkers = List(LineCounterActorTest(linesTestActor, None))
          val distributor = Distributor(file, splitter, numberOfColumns, encoding, listOfWorkers, dropFirsLines = 1, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "lineCounterWithoutTitles")
          distributor ! Start()
          // -1 because we remove one line for the tittles
          linesTestActor.expectMsg(numberOfLines - 1)
        }

        "We will compute the number of lines read when we begin at line 2 (included) and finish at line 6" in {
          val linesTestActor: TestProbe = TestProbe()
          val listOfWorkers = List(LineCounterActorTest(linesTestActor, None))
          val distributor = Distributor(file, splitter, numberOfColumns, encoding, listOfWorkers, dropFirsLines = 1, stopSystemAtTheEnd = false, numberOfLinesPerMessage = 2, suffixNameForTest = "lineCounterReadPartFile", limitNumberOfLinesToRead = Some(5))
          distributor ! Start()
          // use min in case the file is too small.
          linesTestActor.expectMsg((numberOfLines - 1) min 6)
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
