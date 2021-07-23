ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "durak-right-version",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.9",
      "dev.zio" %% "zio-test" % "1.0.9" % Test,
      "dev.zio" %% "zio-logging-slf4j" % "0.5.11",
      "io.d11" %% "zhttp" % "1.0.0.0-RC17"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
