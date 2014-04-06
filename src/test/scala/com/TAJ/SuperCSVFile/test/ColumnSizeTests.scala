package com.TAJ.SuperCSVFile.test

import java.io.File
import com.TAJ.SuperCSVFile.FileStructure.{ LineCounterActorTest, SizeActorInjectedResultActor, FileTools }
import akka.testkit.TestProbe
import com.TAJ.SuperCSVFile.DistributorTest
import org.scalatest.BeforeAndAfterAll
import scalaz._
import Scalaz._
import scala.concurrent.duration._
import scala.concurrent.duration
import scala.Some
import com.TAJ.SuperCSVFile.FileStructure.ColumnSizes
import com.TAJ.SuperCSVFile.ActorMessages.RequestMoreWork

object ColumnSizeTests extends TestTraitAkka with BeforeAndAfterAll {

  val test: ((File, String, String, String, Long, Int, scala.List[Int], scala.List[Int])) ⇒ Unit = {
    case (file, name, encoding, splitter, numberOfLines, numberOfColumns, columnCountWithTitles, columnCountWithoutTitles) ⇒
      s"We will evaluate the column sizes of the file $name." must {

        s"METHOD 1 - The number of columns should be $numberOfColumns" in {
          f ⇒
            val result = FileTools.columnCount(file.getAbsolutePath, splitter, encoding)

            result should be(numberOfColumns)
        }

        s"METHOD 2 - The number of columns should be $numberOfColumns and the splitter should be [$splitter]." in {
          f ⇒
            val (resultSplitter, resultColumns) = FileTools.findColumnDelimiter(file.getAbsolutePath, encoding)

            resultColumns should be(numberOfColumns)
            resultSplitter should be(splitter)
        }

        "The best size of columns including titles will be computed." in {
          f ⇒
            implicit val system = f.system
            val columnSizeTestProbe: TestProbe = TestProbe()
            val columnSizeTestActor = columnSizeTestProbe.ref
            val (testSizeActor, _) = SizeActorInjectedResultActor(columnSizeTestActor, numberOfColumns, splitter)

            val listOfWorkers = List(testSizeActor)
            val distributor = DistributorTest(file, encoding, listOfWorkers, numberOfLinesPerMessage = 2, dropFirsLines = None, limitNumberOfLinesToRead = None)
            distributor ! RequestMoreWork()
            import duration._
            val time = FiniteDuration(10000, SECONDS)
            columnSizeTestProbe.expectMsg(time, ColumnSizes(columnCountWithTitles))
        }

        "The number of lines including titles will be computed." in {
          f ⇒
            implicit val system = f.system
            val linesTestActor: TestProbe = TestProbe()
            val listOfWorkers = List(LineCounterActorTest(linesTestActor, None))
            val distributor = DistributorTest(file, encoding, listOfWorkers, numberOfLinesPerMessage = 2, dropFirsLines = None, limitNumberOfLinesToRead = None)
            distributor ! RequestMoreWork()
            val time = FiniteDuration(10000, SECONDS)
            linesTestActor.expectMsg(time, numberOfLines)
        }

        "The best size of columns without titles will be computed." in {
          f ⇒
            implicit val system = f.system
            val columnSizeTestProbe: TestProbe = TestProbe()
            val columnSizeTestActor = columnSizeTestProbe.ref
            val (testSizeActor, _) = SizeActorInjectedResultActor(columnSizeTestActor, numberOfColumns, splitter)
            val listOfWorkers = List(testSizeActor)
            val distributor = DistributorTest(file, encoding, listOfWorkers, dropFirsLines = Some(1), numberOfLinesPerMessage = 2, limitNumberOfLinesToRead = None)
            distributor ! RequestMoreWork()

            columnSizeTestProbe.expectMsg(ColumnSizes(columnCountWithoutTitles))
        }

        "The number of lines excluding titles will be computed." in {
          f ⇒
            implicit val system = f.system
            val linesTestActor: TestProbe = TestProbe()
            val listOfWorkers = List(LineCounterActorTest(linesTestActor, None))
            val distributor = DistributorTest(file, encoding, listOfWorkers, dropFirsLines = 1.some, numberOfLinesPerMessage = 2, limitNumberOfLinesToRead = None)
            distributor ! RequestMoreWork()
            // -1 because we remove one line for the tittles
            linesTestActor.expectMsg(numberOfLines - 1)
        }

        "We will compute the number of lines read when we begin at line 2 (included) and finish at line 6" in {
          f ⇒
            implicit val system = f.system
            val linesTestActor: TestProbe = TestProbe()
            val listOfWorkers = List(LineCounterActorTest(linesTestActor, None))
            val distributor = DistributorTest(file, encoding, listOfWorkers, dropFirsLines = 1.some, numberOfLinesPerMessage = 2, limitNumberOfLinesToRead = 5.some)
            distributor ! RequestMoreWork()
            // use min in case the file is too small.
            linesTestActor.expectMsg((numberOfLines - 1) min 6)
        }
      }
  }
}
