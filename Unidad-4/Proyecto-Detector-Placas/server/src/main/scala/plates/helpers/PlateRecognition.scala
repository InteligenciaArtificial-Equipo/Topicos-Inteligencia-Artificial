package plates.helpers

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

object PlateRecognition:
    
  val path = "/home/antonio17/Documents/Scala/DetectorPlacas/server/src/main/py/recognize_plate.py"

  def recognizePlate(imageBase64: String): IO[Option[String]] =
    IO {
      try
        val imageBytes = Base64.getDecoder.decode(imageBase64)
        val tempFile = java.io.File.createTempFile("plate_", ".jpg")
        val fos = new java.io.FileOutputStream(tempFile)
        fos.write(imageBytes)
        fos.close()
        
        val result = s"python3 $path ${tempFile.getAbsolutePath}".!!.trim
        tempFile.delete()
        
        if result.nonEmpty then Some(result) else None
      catch
        case e: Exception =>
          println(s"Error en reconocimiento: ${e.getMessage}")
          None
    }