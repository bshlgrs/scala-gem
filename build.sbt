name := "scala-gem"

version := "0.1"

enablePlugins(ScalaJSPlugin)

scalaVersion := "2.12.4"

resolvers ++= List(
  Resolver.sonatypeRepo("releases"),
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.12" % "3.0.0",
  "com.lihaoyi" %%% "fastparse" % "1.0.0",
  "com.lihaoyi" %%% "upickle" % "0.5.1"
)

scalacOptions ++= Seq("scalajs:sjsDefinedByDefault")
