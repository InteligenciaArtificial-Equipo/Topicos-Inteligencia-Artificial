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

/**
 * Definición de todas las rutas HTTP del sistema de detección de placas.
 *
 * Este objeto contiene:
 * - Los decodificadores JSON necesarios.
 * - Los endpoints para registro de placas.
 * - Los endpoints para consulta por imagen.
 */
object Routes {
  
  /**
   * Decodificador JSON para la petición de registro de placas.
   */
  given EntityDecoder[IO, RegisterRequest] = jsonOf[IO, RegisterRequest]
  
  /**
   * Decodificador JSON para la petición de consulta por imagen.
   */
  given EntityDecoder[IO, QueryRequest] = jsonOf[IO, QueryRequest]

  /**
   * Define el conjunto de rutas HTTP del servidor.
   *
   * @param xa Transactor de Doobie para ejecutar las consultas a la base de datos.
   * @return Conjunto de rutas HttpRoutes[IO].
   */
  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    
    /**
       * Endpoint de verificación de estado del servidor (health check).
       *
       * Método: GET
       * Ruta: /health
       * Respuesta: "OK" si el servidor está en funcionamiento.
       */
    case GET -> Root / "health" =>
      Ok("OK")

    /**
       * Endpoint para registrar una placa con su propietario.
       *
       * Método: POST
       * Ruta: /register
       * Body JSON: RegisterRequest
       * Flujo:
       * 1. Se lee el JSON de la petición.
       * 2. Se crea un nuevo propietario.
       * 3. Se crea o actualiza la placa asociada.
       * 4. Se consulta la relación placa-propietario.
       * 5. Se responde con el resultado.
       */
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

    /**
       * Endpoint para consultar el propietario de un vehículo a partir de una imagen.
       *
       * Método: POST
       * Ruta: /query
       * Body JSON: QueryRequest (contiene la imagen en Base64)
       * Flujo:
       * 1. Se decodifica la imagen.
       * 2. Se ejecuta el reconocimiento de placa en Python.
       * 3. Se busca la placa en la base de datos.
       * 4. Se responde con el propietario si existe.
       */
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
}

