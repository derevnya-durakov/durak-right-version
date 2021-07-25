package dev.derevnyadurakov.durakrightversion

import zhttp.http._
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio._
import zio.config._
import ConfigDescriptor._
import zio.config.typesafe.TypesafeConfig
import zio.logging.{LogFormat, LogLevel, Logging, log}

import java.io.File
import scala.util.{Success, Try}

object DurakRightVersion extends App {

  val defaultPort: Either[Int, String] = Left(8080)
  val defaultServerConfig: ServerConfig = ServerConfig(defaultPort)
  val defaultApplicationConfig: ApplicationConfig = ApplicationConfig(defaultServerConfig)

  case class ServerConfig(port: Either[Int, String])

  val serverConfigDescriptor: ConfigDescriptor[ServerConfig] =
    int("port").orElseEither(string("port")).default(defaultPort)(
      ServerConfig.apply,
      ServerConfig.unapply
    )

  case class ApplicationConfig(server: ServerConfig)

  val applicationConfigDescriptor: ConfigDescriptor[ApplicationConfig] =
    nested("server")(serverConfigDescriptor).default(defaultServerConfig)(
      ApplicationConfig.apply,
      ApplicationConfig.unapply
    )

  val configs: TaskLayer[Has[ApplicationConfig]] = {
    val path = getClass.getResource("/application.conf").getPath
    TypesafeConfig.fromHoconFile(new File(path), applicationConfigDescriptor)
  }

  val port: RIO[Has[ApplicationConfig], Int] =
    for {
      config <- getConfig[ApplicationConfig]
      tryPort = config.server.port match {
        case Left(value) => Success(value)
        case Right(value) => Try(value.toInt)
      }
      port <- ZIO.fromTry(tryPort)
    } yield port

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

  private val logger =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName(DurakRightVersion.getClass.getCanonicalName)

  private def server(port: Int) = Server.port(port) ++ Server.app(app +++ routes)

  val infoServerStarted: RIO[Logging with Has[ApplicationConfig], Unit] =
    for {
      p <- port
      _ <- log.info(s"Server started on port $p")
    } yield ()

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    val nThreads = args.headOption.flatMap(x => Try(x.toInt).toOption).getOrElse(0)
    (for {
      p <- port
      _ <- server(p)
        .make
        .use(_ => infoServerStarted *> ZIO.never)
        .provideCustomLayer(ServerChannelFactory.auto ++ EventLoopGroup.auto(nThreads) ++ logger ++ configs)
    } yield ())
      .provideCustomLayer(configs)
      .exitCode
  }

}
