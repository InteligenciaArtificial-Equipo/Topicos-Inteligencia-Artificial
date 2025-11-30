package plates.routes

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

import plates.helpers.PlateRecognition
import plates.daos.*

object Routes:
  given EntityDecoder[IO, RegisterRequest] = jsonOf[IO, RegisterRequest]
  given EntityDecoder[IO, QueryRequest] = jsonOf[IO, QueryRequest]
  given EntityDecoder[IO, QueryByPlateRequest] = jsonOf[IO, QueryByPlateRequest]

  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    
    case GET -> Root / "health" =>
      Ok("OK")

    case req @ POST -> Root / "register" =>
      println("Registrando placa ...")
      for
        registerReq <- req.as[RegisterRequest]
        result <- (for
          ownerId <- Database.createOwner(registerReq.ownerName)
          plateId <- Database.createOrUpdatePlate(registerReq.plateNumber.toUpperCase, ownerId)
          plateWithOwner <- Database.findOwnerByPlate(registerReq.plateNumber.toUpperCase)
        yield plateWithOwner).transact(xa)
        response <- result match
          case Some(data) => Ok(ApiResponse(true, "Placa registrada exitosamente", Some(data)).asJson)
          case None => InternalServerError(ApiResponse(false, "Error al registrar placa").asJson)
      yield response

    case req @ POST -> Root / "query" =>
      println("Consultando ...")
      for
        queryReq <- req.as[QueryRequest]
        plateNumber <- PlateRecognition.recognizePlate(queryReq.imageBase64)
        result <- plateNumber match
          case Some(plate) => Database.findOwnerByPlate(plate.toUpperCase).transact(xa)
          case None => IO.pure(None)
        response <- result match
          case Some(data) => Ok(ApiResponse(true, "Propietario encontrado", Some(data)).asJson)
          case None => NotFound(ApiResponse(false, "Placa no encontrada o no se pudo leer").asJson)
      yield response

  }