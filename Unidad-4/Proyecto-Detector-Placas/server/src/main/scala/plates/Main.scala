package plates

import cats.effect.*
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import com.comcast.ip4s.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.hikari.HikariTransactor
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.parser.*
import org.http4s.circe.*
import java.util.{Base64, UUID}
import scala.sys.process.*

import plates.daos.Database
import plates.routes.*

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Database.transactor.use { xa =>
      val httpApp = CORS.policy.withAllowOriginAll(Routes.routes(xa).orNotFound)
      
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(httpApp)
        .build
        .use(_ => IO.println("Server started at http://localhost:8080") *> IO.never)
        .as(ExitCode.Success)
    }
}