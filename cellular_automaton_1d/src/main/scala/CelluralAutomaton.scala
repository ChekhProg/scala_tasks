import java.io.{File, PrintWriter}
import java.util.concurrent.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.math.pow

case class Tape(t: ArrayBuffer[Boolean]){
  override def toString: String = {
    def cellToString(b : Boolean) : String =
      if (b) "1"
      else "0"
    t.map(cellToString).mkString
  }
}

object Tape{
  def fromString(s : String): Tape = {
    var tape = ArrayBuffer[Boolean]().empty
    for (i <- s){
      val cell = i match {
        case '1' => true
        case '0' => false
      }
      tape += cell
    }
    new Tape(tape)
  }
}

class CellularAutomaton(var startState: Tape) {
  def nextGen(state : Tape): Tape = {
    val tape = state.t
    val curStateMod = tape.last +: tape :+ tape.head
    var newState = ArrayBuffer[Boolean]().empty
    for (i <- 1 to tape.length) {
      val p = curStateMod(i - 1)
      val q = curStateMod(i)
      val r = curStateMod(i + 1)
      newState += (q & (!p)) | (q ^ r)
    }
    Tape(newState)
  }

  def findCycle(): Int = {
    var power = 1
    var lam = 1
    var tortoise = startState
    var hare = nextGen(startState)
    while (tortoise != hare) {
      if (power == lam) {
        tortoise = hare
        power *= 2
        lam = 0
      }
      hare = nextGen(hare)
      lam += 1
      if (lam == 128)
        return 0
    }
    var mu = 0
    tortoise = startState
    hare = startState
    for (i <- 0 until lam) {
      hare = nextGen(hare)
    }
    while (tortoise != hare) {
      tortoise = nextGen(tortoise)
      hare = nextGen(hare)
      mu += 1
    }
    lam
  }

  def printEvolution(n: Int): Unit = {
    var state = startState
    for (i <- 0 to n){
      println(state)
      state = nextGen(state)
    }
  }
}

object CellularAutomaton {
  def toFile(result: ArrayBuffer[(String, Int)]): Unit = {
    val writer = new PrintWriter(new File("test.txt" ))
    result.foreach{
      x => writer.write(x._1.toString + " " + x._2.toString + "\n")
    }
    writer.close()
  }

  def calculateThread(tapeWidth : Int, start : Int, end : Int): ArrayBuffer[(String, Int)] = {
    val result = ArrayBuffer[(String, Int)]().empty

    for (i <- start until end) {
      val s = BigInt(i).toString(2).reverse.padTo(tapeWidth, '0').reverse
      val tape = Tape.fromString(s)
      val auto = new CellularAutomaton(tape)
      val cycleLength = auto.findCycle()
      if (cycleLength != 0)
        result.addOne((s, cycleLength))
    }
    result
  }

  def calculate(): Unit = {

    val threadNumber = 12
    val tapeWight = 14
    val numberOfTapes: Int = pow(2, tapeWight).toInt
    val numberOfTapesPerThread = numberOfTapes / threadNumber
    val result = new Array[ArrayBuffer[(String, Int)]](threadNumber)

    val startTime = System.currentTimeMillis()
    val futures = new Array[java.util.concurrent.Future[_]](threadNumber)
    val ec = Executors.newCachedThreadPool()
    for (i <- 0 until threadNumber){
      val begin = i * numberOfTapesPerThread
      var end = (i + 1) * numberOfTapesPerThread
      if (i == threadNumber - 1)
        end = numberOfTapes
      val task = new Runnable {
        override def run(): Unit =
          result(i) = calculateThread(tapeWight, begin, end)
      }
      val f = ec.submit(task)
      futures(i) = f
    }
    for (i <- 0 until threadNumber){
      futures(i).get()
    }
    ec.shutdown()
    val endTime = System.currentTimeMillis()
    println((endTime - startTime).toDouble/1000)
    val finalResult = ArrayBuffer(result: _*).flatten
    //print(finalResult.groupBy(_._2).keys)
    toFile(finalResult)
    println("Done")
  }
}

object main extends App{
  CellularAutomaton.calculate()
}
