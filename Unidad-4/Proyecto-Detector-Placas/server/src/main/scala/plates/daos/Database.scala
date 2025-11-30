package plates.daos

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

import plates.models.*

object Database {
  /**
   * Crea y configura un transactor de Hikari para conectarse a la base de datos PostgreSQL.
   * El transactor es el que permite ejecutar consultas Doobie dentro del efecto IO.
   *
   * @return Un Resource que administra el ciclo de vida del HikariTransactor[IO]
   */
  def transactor: Resource[IO, HikariTransactor[IO]] =
    for
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql://localhost:5432/plates_detection",
        "postgres",
        "postgres",
        cats.effect.unsafe.IORuntime.global.compute
      )
    yield xa

  /**
   * Busca un vehículo y su propietario a partir del número de placa.
   *
   * @param plateNum Número de placa a buscar.
   * @return Una acción ConnectionIO que devuelve:
   *         - Some(PlateWithOwner) si la placa existe
   *         - None si no hay coincidencias
   */
  def findOwnerByPlate(plateNum: String): ConnectionIO[Option[PlateWithOwner]] =
    sql"""
      SELECT p.id, p.plate_number, p.owner_id,
             o.id, o.name
      FROM plates p
      JOIN owners o ON p.owner_id = o.id
      WHERE p.plate_number = $plateNum
    """
      .query[(UUID, String, UUID, UUID, String)]
      .option
      .map(_.map { case (pid, pn, oid1, oid2, on) =>
        PlateWithOwner(
          Plate(pid, pn, oid1),
          Owner(oid2, on)
        )
      })

  /**
   * Crea un nuevo propietario en la base de datos.
   *
   * @param name Nombre del propietario.
   * @return Una acción ConnectionIO que devuelve el UUID del propietario creado.
   */
  def createOwner(name: String): ConnectionIO[UUID] =
    val id = UUID.randomUUID()
    sql"""
      INSERT INTO owners (id, name)
      VALUES ($id, $name)
    """.update.run.map(_ => id)


  /**
   * Crea una nueva placa o actualiza su propietario si ya existe.
   *
   * Si la placa ya existe, solo se actualiza el owner_id y la fecha de actualización.
   * Si no existe, se inserta un nuevo registro.
   *
   * @param plateNum Número de placa.
   * @param ownerId  UUID del propietario asociado.
   * @return Una acción ConnectionIO que devuelve el UUID de la placa insertada o actualizada.
   */
  def createOrUpdatePlate(plateNum: String, ownerId: UUID): ConnectionIO[UUID] =
    val id = UUID.randomUUID()
    sql"""
      INSERT INTO plates (id, plate_number, owner_id)
      VALUES ($id, $plateNum, $ownerId)
      ON CONFLICT (plate_number) 
      DO UPDATE SET owner_id = $ownerId, updated_at = NOW()
      RETURNING id
    """.query[UUID].unique
}
