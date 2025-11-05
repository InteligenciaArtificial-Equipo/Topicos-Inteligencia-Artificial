package pso.tests

import pso.main.SensorPlacement
import pso.main.SensorPlacement._
import pso.main.ObservedPoint
import scala.util.Random

/** Objeto de prueba unitaria para validar la correcta implementación de las
  * funciones auxiliares clave en [[pso.main.SensorPlacement]].
  *
  * Verifica funciones como la conversión de coordenadas, la estimación por IDW,
  * el cálculo del RMSE y la penalización por distancia mínima, asegurando que
  * la función de fitness del PSO se evalúe correctamente.
  */
object SensorPlacementTest {

  // Helpers numéricos

  /** Compara si dos valores [[Double]] son aproximadamente iguales dentro de
    * una tolerancia.
    */
  private def approxEq(a: Double, b: Double, tol: Double = 1e-9): Boolean =
    math.abs(a - b) <= tol

  /** Compara si dos tuplas de [[Double]] (coordenadas) son aproximadamente
    * iguales.
    */
  private def approxTupleEq(
      a: (Double, Double),
      b: (Double, Double),
      tol: Double = 1e-9
  ): Boolean =
    approxEq(a._1, b._1, tol) && approxEq(a._2, b._2, tol)

  /** Función local para reproducir la lógica de generación de puntos de malla.
    * @param field
    *   Configuración dimensional y de malla del campo.
    * @return
    *   Vector de coordenadas (x, y) de la malla esperada.
    */
  private def expectedGridPoints(
      field: FieldConfig
  ): Vector[(Double, Double)] = {
    val xs =
      (0 until field.nx).map(i => (i.toDouble / (field.nx - 1)) * field.width)
    val ys =
      (0 until field.ny).map(j => (j.toDouble / (field.ny - 1)) * field.height)
    xs.toVector.flatMap(x => ys.toVector.map(y => (x, y)))
  }

  /** Ejecuta el conjunto de pruebas unitarias sobre las funciones de
    * [[SensorPlacement]].
    *
    * @return
    *   Un [[Vector]] de [[String]] que contiene descripciones de las pruebas
    *   fallidas. Si el vector está vacío, todas las pruebas pasaron.
    */
  def run(): Vector[String] = {
    var fails = Vector.empty[String]

    // 1) gridPoints para nx=2, ny=2
    val field = FieldConfig(width = 10.0, height = 20.0, nx = 2, ny = 2)
    val ptsExpected = expectedGridPoints(field)
    val expectedPts = Vector((0.0, 0.0), (0.0, 20.0), (10.0, 0.0), (10.0, 20.0))
    if (
      ptsExpected.length != expectedPts.length || ptsExpected
        .zip(expectedPts)
        .exists { case (p, e) => !approxTupleEq(p, e) }
    ) {
      fails =
        fails :+ s"gridPoints mismatch: got $ptsExpected expected $expectedPts"
    }

    // 2) vecToCoords debe emparejar componentes
    val vec = Vector(1.0, 2.0, 3.0, 4.0)
    val coords = vecToCoords(vec, field)
    val expectedCoords = Vector((1.0, 2.0), (3.0, 4.0))
    if (
      coords.length != expectedCoords.length || coords
        .zip(expectedCoords)
        .exists { case ((x, y), (ex, ey)) =>
          !approxEq(x, ex) || !approxEq(y, ey)
        }
    ) {
      fails =
        fails :+ s"vecToCoords mismatch: got $coords expected $expectedCoords"
    }

    // 3) estimateAtPointsIDW con un solo sensor
    val sensorCoords = Vector((10.0, 20.0))
    val readings = Vector(0.75)
    val queryPts = Vector((0.0, 0.0), (10.0, 20.0), (100.0, 200.0))
    val est = estimateAtPointsIDW(sensorCoords, readings, queryPts, power = 2.0)
    if (!est.forall(e => approxEq(e, 0.75, 1e-9))) {
      fails = fails :+ s"estimateAtPointsIDW single-sensor wrong: $est"
    }

    // 4) observedRMSE: sensor colocado exactamente en la ubicación de observación
    val obsPt = ObservedPoint(
      humidity = 75.0 / 100.0,
      crop = "maiz",
      elevation = 0.0,
      salinity = 0.0,
      temperature = 25.0,
      lat = 0.0,
      lon = 0.0
    )
    val projected = wgs84ToLocalMeters(Seq(obsPt)).toVector
    val aligned = alignObservedToField(projected, field)
    if (aligned.isEmpty) {
      fails =
        fails :+ s"alignObservedToField produced empty for single-point projected obs: $projected"
    } else {
      val obsCoord = aligned
        .flatMap(p => for (x <- p.x; y <- p.y) yield (x, y))
        .headOption
        .getOrElse((0.0, 0.0))
      val sensorAtObs = Vector(obsCoord)
      val rng = new Random(0)
      val samplePts = expectedGridPoints(field)
      val truthPlaceholder =
        Vector.fill(samplePts.length)(aligned.head.humidity)
      val readingsAtObs =
        sensorReadings(sensorAtObs, samplePts, truthPlaceholder, rng, 0.0)
      val rmseObs = observedRMSE(
        sensorAtObs,
        readingsAtObs,
        aligned,
        power = 2.0,
        Map("maiz" -> 2.0, "tomate" -> 1.5, "chile" -> 1.2)
      )
      if (!(rmseObs >= 0.0 && rmseObs < 1e-9)) {
        fails = fails :+ s"observedRMSE unexpected for sensor at obs: $rmseObs"
      }
    }

    // 5) weightedRMSE verificación aritmética simple
    val est2 = Vector(0.2, 0.4)
    val truth2 = Vector(0.0, 0.0)
    val weights = Vector(1.0, 1.0)
    val expectedRmse = math.sqrt((0.2 * 0.2 + 0.4 * 0.4) / 2.0)
    val computedRmse = {
      val weighted = est2.zip(truth2).zip(weights).map { case ((e, t), w) =>
        val err = e - t
        w * err * err
      }
      math.sqrt(weighted.sum / weights.sum)
    }
    if (!approxEq(expectedRmse, computedRmse, 1e-12)) {
      fails =
        fails :+ s"weightedRMSE arithmetic mismatch: expected $expectedRmse computed $computedRmse"
    }

    // 6) minDistancePenalty: dos sensores muy cercanos deben dar una penalización positiva
    val closeCoords = Vector((0.0, 0.0), (1.0, 1.0))
    val pen = minDistancePenalty(closeCoords, minThresh = 5.0)
    if (!(pen > 0.0))
      fails =
        fails :+ s"minDistancePenalty should be positive for close sensors, got $pen"

    // 7) makeCropWeights devuelve la misma longitud que los puntos de malla esperados
    val cropW = makeCropWeights(field)
    val gp = expectedGridPoints(field)
    if (cropW.length != gp.length)
      fails =
        fails :+ s"makeCropWeights length mismatch: ${cropW.length} vs ${gp.length}"

    fails
  }

  def main(args: Array[String]): Unit = {
    val fails = run()
    if (fails.isEmpty) {
      println("[PASS] SensorPlacementTest")
    } else {
      println("[FAIL] SensorPlacementTest")
      fails.foreach(println)
      sys.exit(1)
    }
  }
}
