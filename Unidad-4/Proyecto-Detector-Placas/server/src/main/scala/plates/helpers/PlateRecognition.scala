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


/**
 * Objeto encargado de la comunicación con el script de Python
 * que realiza el reconocimiento de placas vehiculares.
 */
object PlateRecognition {
    
  /**
   * Ruta absoluta del script de Python que ejecuta el reconocimiento OCR.
   */
  val path = "/home/antonio17/Documents/Scala/DetectorPlacas/server/src/main/py/recognize_plate.py"

  /**
   * Realiza el reconocimiento de una placa a partir de una imagen codificada en Base64.
   * 
   * El proceso consiste en:
   * 1. Decodificar la imagen desde Base64.
   * 2. Guardarla en un archivo temporal.
   * 3. Ejecutar el script de Python pasándole la ruta del archivo.
   * 4. Leer el resultado del reconocimiento.
   * 5. Eliminar el archivo temporal.
   *
   * @param imageBase64 Imagen codificada en formato Base64.
   * @return Un IO que devuelve:
   *         - Some(String) con el número de placa reconocida si fue exitoso.
   *         - None si ocurre un error o no se reconoce ninguna placa.
   */
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
}
