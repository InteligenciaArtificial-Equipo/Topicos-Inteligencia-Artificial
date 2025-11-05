package pso.main

import pso.core.*
import pso.main.VectorDoubleOps
import scala.util.Random
import scala.io.Source
import scala.util.Try

/** Motor principal para la optimización de la colocación de sensores de humedad
  * mediante PSO.
  *
  * El objetivo es encontrar las coordenadas de $K$ sensores que minimicen una
  * función de fitness compuesta por el error cuadrático medio (RMSE) de la
  * estimación de humedad por Distancia Inversa Ponderada (IDW) sobre puntos de
  * observación, más una penalización por la proximidad excesiva entre sensores.
  *
  *   - Modo de operación: Obligatorio en modo CSV, requiere un archivo de
  *     observaciones para inicializar el problema.
  *   - Reproducibilidad: Usa un [[Random]] con semilla fija para asegurar que
  *     las lecturas simuladas de sensores sean reproducibles.
  */
object SensorPlacement {

  /** Define las dimensiones físicas del campo de estudio y la resolución de la
    * malla.
    * @param width
    *   Ancho del campo en metros.
    * @param height
    *   Altura del campo en metros.
    * @param nx
    *   Número de puntos de la malla en la dimensión X.
    * @param ny
    *   Número de puntos de la malla en la dimensión Y.
    */
  case class FieldConfig(width: Double, height: Double, nx: Int, ny: Int)

  /** Define los parámetros que rigen el problema de optimización y la
    * simulación.
    * @param field
    *   Configuración dimensional y de malla del campo.
    * @param baseMoisture
    *   Humedad base (no usada directamente en este código, pero relevante en
    *   modelos sintéticos).
    * @param elevationEffect
    *   Efecto de la elevación (no usado directamente en este código, pero
    *   relevante en modelos sintéticos).
    * @param sensorNoiseSigma
    *   Desviación estándar sigma del ruido Gaussiano aplicado a la lectura
    *   simulada del sensor.
    * @param cropImportanceLevels
    *   Mapa que asigna un peso de importancia a cada tipo de cultivo o zona.
    * @param alphaPenalty
    *   Coeficiente alpha que pondera la penalización por distancia mínima en la
    *   función de fitness.
    * @param minDistThresh
    *   Distancia mínima requerida entre dos sensores para evitar penalización.
    * @param idwPower
    *   Exponente p usado en la interpolación por Distancia Inversa Ponderada
    *   (IDW).
    */
  case class SimulationConfig(
      field: FieldConfig,
      nBumps: Int = 0,
      baseMoisture: Double = 0.25,
      elevationEffect: Double = 0.1,
      sensorNoiseSigma: Double = 0.02,
      cropImportanceLevels: Map[String, Double] =
        Map("maiz" -> 2.0, "tomate" -> 1.5, "chile" -> 1.2),
      alphaPenalty: Double = 1.0,
      minDistThresh: Double = 10.0,
      idwPower: Double = 2.0
  )

  /** Carga los datos de observación de humedad y características del terreno
    * desde un archivo CSV.
    *
    * La función realiza un parseo estricto y normaliza el valor de humedad de
    * porcentaje (0-100) al rango [0, 1]. Termina la ejecución si el archivo no
    * es accesible o está vacío.
    *
    * @param path
    *   Ruta del archivo CSV de entrada.
    * @return
    *   Un [[Vector]] de [[ObservedPoint]] válidos.
    */
  def loadObservedCsv(path: String): Vector[ObservedPoint] = {
    val lines = Try(Source.fromFile(path).getLines().toVector).getOrElse {
      sys.error(s"ERROR: no se pudo leer el CSV en $path")
    }
    if (lines.isEmpty) sys.error(s"ERROR: CSV en $path está vacío")
    val header = lines.head.split(",").map(_.trim.toLowerCase)
    val rows = lines.tail.flatMap { ln =>
      val cols = ln.split(",").map(_.trim)
      if (cols.length < 7) None
      else {
        val hum = Try(cols(0).toDouble / 100.0).getOrElse(Double.NaN)
        val crop = cols(1)
        val elev = Try(cols(2).toDouble).getOrElse(Double.NaN)
        val sal = Try(cols(3).toDouble).getOrElse(Double.NaN)
        val temp = Try(cols(4).toDouble).getOrElse(Double.NaN)
        val lat = Try(cols(5).toDouble).getOrElse(Double.NaN)
        val lon = Try(cols(6).toDouble).getOrElse(Double.NaN)
        if (hum.isNaN || lat.isNaN || lon.isNaN) None
        else Some(ObservedPoint(hum, crop, elev, sal, temp, lat, lon))
      }
    }.toVector
    rows
  }

  /** Proyecta las coordenadas geográficas WGS84 (lat/lon) a coordenadas locales
    * planas (metros).
    *
    * Utiliza una aproximación equirrectangular simple, tomando el centroide de
    * las observaciones como punto de referencia lat, lon.
    *
    * @param obs
    *   Secuencia de puntos de observación con coordenadas lat/lon.
    * @return
    *   Secuencia de [[ObservedPoint]] con los campos `x` e `y` rellenados.
    */
  def wgs84ToLocalMeters(obs: Seq[ObservedPoint]): Seq[ObservedPoint] = {
    val R = 6371000.0
    val lat0 = obs.map(_.lat).sum / obs.length
    val lon0 = obs.map(_.lon).sum / obs.length
    val lat0rad = math.toRadians(lat0)
    obs.map { p =>
      val dLat = math.toRadians(p.lat - lat0)
      val dLon = math.toRadians(p.lon - lon0)
      val x = R * dLon * math.cos(lat0rad)
      val y = R * dLat
      p.copy(x = Some(x), y = Some(y))
    }
  }

  /** Normaliza y escala las coordenadas locales de los puntos de observación al
    * espacio de búsqueda definido por [[FieldConfig]].
    *
    * Realiza una traslación y escalado uniforme para ajustar el conjunto de
    * puntos al rectángulo `[0, width] x [0, height]`, con un truncamiento para
    * asegurar que todos los puntos caigan dentro del campo.
    *
    * @param obs
    *   Vector de puntos de observación con coordenadas locales `x`/`y`.
    * @param field
    *   Configuración del campo que define el ancho y alto del espacio de
    *   búsqueda.
    * @return
    *   Vector de [[ObservedPoint]] con coordenadas locales alineadas.
    */
  def alignObservedToField(
      obs: Vector[ObservedPoint],
      field: FieldConfig
  ): Vector[ObservedPoint] = {
    val xs = obs.flatMap(_.x); val ys = obs.flatMap(_.y)
    if (xs.isEmpty || ys.isEmpty) obs
    else {
      val minX = xs.min; val maxX = xs.max
      val minY = ys.min; val maxY = ys.max
      val eps = 1e-9
      val scaleX =
        if (math.abs(maxX - minX) < eps) 1.0 else (field.width / (maxX - minX))
      val scaleY =
        if (math.abs(maxY - minY) < eps) 1.0 else (field.height / (maxY - minY))
      obs.map { p =>
        (p.x, p.y) match {
          case (Some(px), Some(py)) =>
            val nx = (px - minX) * scaleX
            val ny = (py - minY) * scaleY
            p.copy(
              x = Some(nx.max(0.0).min(field.width)),
              y = Some(ny.max(0.0).min(field.height))
            )
          case _ => p
        }
      }.toVector
    }
  }

  // --- IDW & RMSE for observed points --------------------------------------

  /** Estima el valor de una variable (humedad) en un conjunto de puntos usando
    * Interpolación por Distancia Inversa Ponderada (IDW).
    *
    * @param sensorCoords
    *   Coordenadas (x, y) de los sensores.
    * @param readings
    *   Lecturas de humedad de los sensores correspondientes.
    * @param pts
    *   Coordenadas (x, y) de los puntos donde se desea la estimación.
    * @param power
    *   Exponente de ponderación p (IDW).
    * @param eps
    *   Pequeño valor para evitar la división por cero si la distancia es cero.
    * @return
    *   Un [[Vector]] de valores estimados en los puntos `pts`.
    */
  def estimateAtPointsIDW(
      sensorCoords: Vector[(Double, Double)],
      readings: Vector[Double],
      pts: Vector[(Double, Double)],
      power: Double,
      eps: Double = 1e-6
  ): Vector[Double] =
    pts.map { case (x, y) =>
      val ws = sensorCoords.zip(readings).map { case ((sx, sy), rv) =>
        val dx = x - sx; val dy = y - sy
        val dist2 = dx * dx + dy * dy + eps
        val w = 1.0 / math.pow(dist2, power / 2.0)
        (w, rv)
      }
      val num = ws.map { case (w, v) => w * v }.sum
      val den = ws.map(_._1).sum
      if (den == 0.0) 0.0 else num / den
    }

  /** Calcula el Error Cuadrático Medio Ponderado (RMSE) de la estimación IDW
    * sobre los puntos de observación.
    *
    * El error se pondera por la importancia del cultivo en cada punto, siendo
    * la métrica principal de la función de fitness (minimización).
    *
    * @param sensorCoords
    *   Coordenadas (x, y) de los sensores.
    * @param readings
    *   Lecturas simuladas de humedad de los sensores.
    * @param obs
    *   Puntos de observación reales [[ObservedPoint]].
    * @param power
    *   Exponente p de IDW.
    * @param cropWeights
    *   Mapa de pesos de importancia por tipo de cultivo.
    * @return
    *   El valor del RMSE ponderado.
    */
  def observedRMSE(
      sensorCoords: Vector[(Double, Double)],
      readings: Vector[Double],
      obs: Vector[ObservedPoint],
      power: Double,
      cropWeights: Map[String, Double]
  ): Double = {
    val obsPts =
      obs.flatMap(p => for (x <- p.x; y <- p.y) yield (x, y)).toVector
    if (obsPts.isEmpty) 0.0
    else {
      val est = estimateAtPointsIDW(sensorCoords, readings, obsPts, power)
      val truths = obs.map(_.humidity)
      val weights = obs.map(p => cropWeights.getOrElse(p.crop.toLowerCase, 1.0))
      val weighted = est.zip(truths).zip(weights).map { case ((e, t), w) =>
        w * (e - t) * (e - t)
      }
      math.sqrt(weighted.sum / weights.sum)
    }
  }

  /** Simula las lecturas de humedad que obtendrían los sensores en sus
    * coordenadas.
    *
    * Las lecturas se obtienen tomando el valor real (truth) del punto de malla
    * más cercano a la ubicación del sensor y añadiéndole ruido Gaussiano.
    *
    * @param sensorCoords
    *   Coordenadas (x, y) de los sensores.
    * @param pts
    *   Coordenadas de la malla de puntos de referencia (grid).
    * @param trueVals
    *   Valores reales de humedad en cada punto de la malla.
    * @param rng
    *   Generador de números aleatorios para el ruido.
    * @param noiseSigma
    *   Desviación estándar del ruido.
    * @return
    *   Un [[Vector]] de [[Double]] con las lecturas simuladas (acotadas a [0,
    *   1]).
    */
  def sensorReadings(
      sensorCoords: Vector[(Double, Double)],
      pts: Vector[(Double, Double)],
      trueVals: Vector[Double],
      rng: Random,
      noiseSigma: Double
  ): Vector[Double] = {
    def valueAt(x: Double, y: Double): Double = {
      val idx = pts.zipWithIndex.minBy { case ((gx, gy), _) =>
        val dx = gx - x; val dy = gy - y; dx * dx + dy * dy
      }._2
      trueVals(idx)
    }

    // Box-Muller con rng
    sensorCoords.map { case (x, y) =>
      val mean = valueAt(x, y)
      val u = rng.nextDouble()
      val v = rng.nextDouble()
      val z = math.sqrt(-2.0 * math.log(math.max(1e-12, u))) * math.cos(
        2.0 * math.Pi * v
      )
      val reading = mean + z * noiseSigma
      if (reading < 0.0) 0.0 else if (reading > 1.0) 1.0 else reading
    }
  }

  /** Calcula una penalización cuadrática si algún par de sensores está
    * demasiado cerca.
    *
    * La penalización se activa si la distancia entre dos sensores es menor que
    * `minThresh`.
    *
    * @param sensorCoords
    *   Coordenadas (x, y) de los sensores.
    * @param minThresh
    *   Distancia mínima permitida.
    * @return
    *   La suma cuadrática de las deficiencias de distancia.
    */
  def minDistancePenalty(
      sensorCoords: Vector[(Double, Double)],
      minThresh: Double
  ): Double = {
    val pairs = for {
      i <- sensorCoords.indices
      j <- (i + 1) until sensorCoords.length
    } yield {
      val (x1, y1) = sensorCoords(i)
      val (x2, y2) = sensorCoords(j)
      val d = math.hypot(x1 - x2, y1 - y2)
      math.max(0.0, minThresh - d)
    }
    pairs.map(s => s * s).sum
  }

  /** Convierte un vector unidimensional de posiciones de partícula (2K) a un
    * vector de tuplas de coordenadas (K).
    * @param vec
    *   Vector de dobles (x1, y1, x2, y2, ...).
    * @param field
    *   Configuración del campo (usada para referencia).
    * @return
    *   Un [[Vector]] de pares (x, y) que representan las coordenadas de los K
    *   sensores.
    */
  def vecToCoords(
      vec: Vector[Double],
      field: FieldConfig
  ): Vector[(Double, Double)] =
    vec.grouped(2).collect { case Seq(x, y) => (x, y) }.toVector

  /** Construye la función de fitness (objetivo a minimizar) del problema de
    * optimización.
    *
    * La función toma un vector de coordenadas de sensores, calcula el RMSE
    * ponderado de la estimación sobre los puntos de observación y añade una
    * penalización por distancia.
    *
    * @param cfg
    *   La configuración de la simulación.
    * @param fieldCfg
    *   La configuración del campo.
    * @param pts
    *   Coordenadas de la malla de referencia.
    * @param truth
    *   Valores reales de humedad en la malla de referencia (usados para simular
    *   lecturas).
    * @param cropWeights
    *   Vector de pesos por cultivo (aunque se usa `cropWeightsMap` interno).
    * @param observed
    *   Puntos de observación alineados.
    * @return
    *   Una función [[V -> Double]] que representa la función de fitness.
    */
  def makeFitnessWithObs(
      cfg: SimulationConfig,
      fieldCfg: FieldConfig,
      pts: Vector[(Double, Double)],
      truth: Vector[Double],
      cropWeights: Vector[Double],
      observed: Vector[ObservedPoint]
  ): Vector[Double] => Double = {

    val cropWeightsMap = Map("maiz" -> 2.0, "tomate" -> 1.5, "chile" -> 1.2)

    vec =>
      val coords = vec
        .grouped(2)
        .collect { case Seq(x, y) =>
          val xc = x.max(0.0).min(fieldCfg.width)
          val yc = y.max(0.0).min(fieldCfg.height)
          (xc, yc)
        }
        .toVector

      val seed = (coords.map { case (x, y) =>
        java.lang.Double.doubleToLongBits(x + y)
      }.sum % 1000000007L).toLong + 1L
      val rngLocal = new Random(seed)

      val readings =
        sensorReadings(coords, pts, truth, rngLocal, cfg.sensorNoiseSigma)

      val rmseObs =
        observedRMSE(coords, readings, observed, cfg.idwPower, cropWeightsMap)
      val penalty = minDistancePenalty(coords, cfg.minDistThresh)

      val fitness = 1.0 * rmseObs + cfg.alphaPenalty * penalty
      fitness
  }

  /** Genera pesos sintéticos de importancia para la malla basados en la
    * asignación de cultivos.
    *
    * Esta función se utiliza para simular zonas de distinta importancia cuando
    * no se tienen datos reales de pesos por ubicación.
    *
    * @param field
    *   Configuración dimensional del campo.
    * @return
    *   Un [[Vector]] de pesos de importancia, uno por punto de la malla.
    */
  def makeCropWeights(field: FieldConfig): Vector[Double] = {
    val pts = {
      val xs =
        (0 until field.nx).map(i => (i.toDouble / (field.nx - 1)) * field.width)
      val ys = (0 until field.ny).map(j =>
        (j.toDouble / (field.ny - 1)) * field.height
      )
      xs.toVector.flatMap(x => ys.toVector.map(y => (x, y)))
    }

    val centers = Vector(
      (field.width * 0.25, field.height * 0.25, "maiz"),
      (field.width * 0.75, field.height * 0.25, "tomate"),
      (field.width * 0.5, field.height * 0.75, "chile")
    )
    pts.map { case (x, y) =>
      val (cx, cy, crop) = centers.minBy { case (cx0, cy0, _) =>
        val dx = x - cx0; val dy = y - cy0; dx * dx + dy * dy
      }
      crop match {
        case "maiz"   => 2.0
        case "tomate" => 1.5
        case "chile"  => 1.2
        case _        => 1.0
      }
    }
  }
}
