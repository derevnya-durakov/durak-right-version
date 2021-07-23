package dev.derevnyadurakov.durakrightversion

import zhttp.http._
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio.logging.{LogFormat, LogLevel, Logging, log}
import zio._

import scala.util.Try

object DurakRightVersion extends App {

  private val port = 8080

  private val app: UHttpApp = Http.collect {
    case Method.GET -> Root / "hello" =>
      Response.text("Hello world!")
    case Method.GET -> Root / "hello" / name =>
      Response.text(s"Hello, $name!")
  }

  private val routes = Http.collectM[Request] {
    case Method.GET -> Root / "utc" =>
      for {
        dateTime <- clock.currentDateTime
        _ <- log.info(s"utc request at $dateTime")
      } yield Response.text(dateTime.toString)
  }

  private val server =
    Server.port(port) ++
      Server.app(app +++ routes)

  private val logger =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName(DurakRightVersion.getClass.getCanonicalName)

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    val nThreads = args.headOption.flatMap(x => Try(x.toInt).toOption).getOrElse(0)
    server
      .make
      .use(_ => log.info(s"Server started on port $port") *> ZIO.never)
      .provideCustomLayer(ServerChannelFactory.auto ++ EventLoopGroup.auto(nThreads) ++ logger)
      .exitCode
  }

}
