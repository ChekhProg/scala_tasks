name := "scala_tasks"

version := "0.1"

scalaVersion := "2.13.6"

lazy val cellular_automaton_1d = project.settings(
  scalaVersion := "2.13.6"
)

lazy val akkaVersion    = "2.6.14"
lazy val akkaHttpVersion = "10.2.3"

lazy val cellular_automaton_2d = project.settings(
  scalaVersion := "2.13.6",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
  )
)

lazy val wc_grep = project.settings(
  scalaVersion := "3.0.0",
  libraryDependencies ++= Seq(
    "co.fs2" %% "fs2-core" % "3.0.4",
    "co.fs2" %% "fs2-io" % "3.0.4",
    "com.novocode" % "junit-interface" % "0.11" % "test",
    "org.scalatest" %% "scalatest" % "3.2.9" % "test",
    "com.novocode" % "junit-interface" % "0.8" % "test->default",
  )
)