
import akka.actor.typed.scaladsl.TimerScheduler
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.Array.ofDim
import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scala.util.Random


class CellularAutomaton2D(val universe: Universe) {

  val n: Int = universe.values.length
  val cells: Array[Array[ActorRef[Cell.Command]]] = ofDim(n, n)
  val values: Array[Array[Boolean]] = universe.values
  val rand = new Random(23534859)

  object Cell {
    sealed trait Command
    sealed trait WatcherCommand

    case object Timeout extends Command
    case object Stop extends Command
    case object Start extends Command
    case class StatusToCell(status: Boolean, from: ActorRef[Command]) extends Command

    case class StatusToWatcher(status: Boolean, coordinates: (Int, Int)) extends WatcherCommand
    case object Print extends WatcherCommand
    case object StopWatcher extends WatcherCommand


    def apply(x: Int, y: Int, s: Boolean, delay: FiniteDuration, watcher: ActorRef[WatcherCommand]): Behavior[Cell.Command] = {
      Behaviors.withTimers(timers => new Cell(x, y, s, delay, timers, watcher).await())
    }
  }

  class Cell(x: Int, y: Int, var s: Boolean,
             delay: FiniteDuration,
             timers: TimerScheduler[Cell.Command],
             watcher: ActorRef[Cell.WatcherCommand]) {
    import Cell._

    val neighbors: mutable.Map[ActorRef[Cell.Command], Boolean] = mutable.Map[ActorRef[Cell.Command], Boolean]().empty

    def await(): Behavior[Command] =
      Behaviors.receive {
        case (context, Start) =>
          for (i <- -1 to 1; j <- -1 to 1; if !(i==j && i==0)){
            var iCur = x + i
            var jCur = y + j
            if (iCur == -1) iCur = n-1
            if (iCur == n) iCur = 0
            if (jCur == -1) jCur = n-1
            if (jCur == n) jCur = 0
            neighbors += (cells(iCur)(jCur) -> values(iCur)(jCur))
            watcher ! StatusToWatcher(s, (x, y))
          }
          timers.startTimerWithFixedDelay(Timeout, delay)
          listen()
      }

    def listen(): Behavior[Command] =
      Behaviors.receive {
        case (context, StatusToCell(status, from)) =>
          neighbors(from) = status
          listen()
        case (context, Timeout) =>
          val liveNeighbors = neighbors.values.count(x => x)
          (s, liveNeighbors) match {
            case (true, 2) => s = true
            case (true, 3) => s = true
            case (false, 3) => s = true
            case (false, _) =>
              if (rand.nextInt(100) == 0)
                s = true
              else
                s = false
            case (_, _) => s = false
          }
          neighbors.keys.foreach(_ ! StatusToCell(s, context.self))
          watcher ! StatusToWatcher(s, (x, y))
          listen()
        case (context, Stop) =>
          Behaviors.stopped
      }
  }


  object Watcher {
    import Cell._

    val newUniverse: Universe = Universe(universe.values)

    def apply(): Behavior[WatcherCommand] =
      Behaviors.setup { context =>

        val watcher = context.self

        for (i <- 0 until n; j <- 0 until n) {
          val c = context.spawn(Cell(i, j, values(i)(j), new FiniteDuration(10, MILLISECONDS), watcher), s"$i:$j")
          cells(i)(j) = c
        }
        cells.flatten.foreach{
          x => x ! Start
        }
        listen()
      }

    def listen(): Behavior[WatcherCommand] =
      Behaviors.receiveMessage[WatcherCommand] {
        case StatusToWatcher(status, (x, y)) =>
          newUniverse.values(x)(y) = status
          listen()
        case Print =>
          println(newUniverse)
          listen()
        case StopWatcher =>
          Behaviors.stopped
      }
  }
}

object CellularAutomaton2D extends App {
  import Universe._
  val un = Universe.fromString(
    """010000
      |001000
      |111000
      |000000
      |000000
      |000000""".stripMargin)
  val automaton2D = new CellularAutomaton2D(un)

  val main: ActorSystem[automaton2D.Cell.WatcherCommand] = ActorSystem(automaton2D.Watcher(), "guard")

  Thread.sleep(80)
  main ! automaton2D.Cell.Print

  Thread.sleep(1000)

  main ! automaton2D.Cell.StopWatcher
}

