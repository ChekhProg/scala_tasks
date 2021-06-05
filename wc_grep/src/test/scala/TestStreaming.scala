
import org.junit.Test
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.effect.SyncIO
import fs2.{Pipe, Stream, io, text}

import java.io.InputStream
import cats.effect.unsafe.implicits.global
import fs2.io.file.Files

import java.nio.file.Paths
import scala.util.matching.Regex

class TestLinuxCommand:
  object WC:
    def resourceIO(name: String): IO[InputStream] =
      IO{getClass.getResourceAsStream(name)}

    val largeFile: Stream[IO, Byte] = {
      Files[IO].readAll(Paths.get("src/test/resources/large-file.txt"), 4096)
    }
    val wordRegEx = raw"[a-zA-Z]+".r

    def words: Pipe[IO, String, String] =
      in => in.flatMap{ line =>
        Stream.emits(wordRegEx
          .findAllIn(line)
          .map(_.toString)
          .toList)
      }

    val largeFileLines = largeFile
      .through(text.utf8Decode)
      .through(text.lines)

    case class Counter(linesCount: Int, wordCount: Int, charCount: Int)

    def combine(a: Counter, b: Counter) =
      Counter(a.linesCount + b.linesCount, a.wordCount + b.wordCount, a.charCount + b.charCount)

    def countWordsChars(line: String): Counter =
      Stream.emits(wordRegEx.findAllIn(line).map(_.toString).toList)
        .map(x => Counter(0, 1, x.length))
        .fold(Counter(1, 0, 0))(combine).compile.toList.head

    def count: IO[Counter] =
      largeFileLines
        .map(countWordsChars)
        .reduce(combine)
        .compile.toList.map(_.head)

  object GREP:
    def resourceIO(name: String): IO[InputStream] =
      IO{getClass.getResourceAsStream(name)}

    val largeFile: Stream[IO, Byte] = {
      Files[IO].readAll(Paths.get("src/test/resources/large-file.txt"), 4096)
    }
    val wordRegEx = raw"[a-zA-Z]+".r

    val largeFileLines = largeFile
      .through(text.utf8Decode)
      .through(text.lines)

    def find(reg: Regex) =
      largeFileLines
        .filter(line => reg.findFirstIn(line).isDefined)
        .foreach(i => IO(println(i)))
        .compile.drain

  @Test def testLinuxCommand: Unit =
    val cnt = WC.count.unsafeRunSync()
    println(cnt)
    val reg = "ipsum"
    GREP.find(reg.r).unsafeRunSync()
