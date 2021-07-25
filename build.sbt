ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.1.0"

val zioVersion = "1.0.9"
val zioConfigVersion = "1.0.6"

lazy val root = (project in file("."))
  .settings(
    name := "durak-right-version",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-logging-slf4j" % "0.5.11",
      "io.d11" %% "zhttp" % "1.0.0.0-RC17"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
