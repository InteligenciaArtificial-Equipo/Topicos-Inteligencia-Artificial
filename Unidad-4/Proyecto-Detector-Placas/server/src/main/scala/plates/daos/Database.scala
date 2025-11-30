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

object Database:
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

  def createOwner(name: String): ConnectionIO[UUID] =
    val id = UUID.randomUUID()
    sql"""
      INSERT INTO owners (id, name)
      VALUES ($id, $name)
    """.update.run.map(_ => id)

  def createOrUpdatePlate(plateNum: String, ownerId: UUID): ConnectionIO[UUID] =
    val id = UUID.randomUUID()
    sql"""
      INSERT INTO plates (id, plate_number, owner_id)
      VALUES ($id, $plateNum, $ownerId)
      ON CONFLICT (plate_number) 
      DO UPDATE SET owner_id = $ownerId, updated_at = NOW()
      RETURNING id
    """.query[UUID].unique