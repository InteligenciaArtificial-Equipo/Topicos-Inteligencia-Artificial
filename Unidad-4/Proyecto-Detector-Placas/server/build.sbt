name := "plates-detection-api"
version := "0.1"
scalaVersion := "3.3.1"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-server" % "0.23.23",
  "org.http4s" %% "http4s-ember-client" % "0.23.23",
  "org.http4s" %% "http4s-dsl" % "0.23.23",
  "org.http4s" %% "http4s-circe" % "0.23.23",
  
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6",
  
  "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
  "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC4",
  "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC4",
  
  "org.typelevel" %% "cats-effect" % "3.5.2",
  "org.typelevel" %% "cats-core" % "2.10.0",
  
  "org.slf4j" % "slf4j-simple" % "2.0.9",

  "org.http4s" %% "http4s-server" % "0.23.23",
  "org.http4s" %% "http4s-core" % "0.23.23"

)