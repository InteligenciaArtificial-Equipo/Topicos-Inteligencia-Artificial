//> using dep "org.scalanlp::breeze-viz:2.1.0"
//> using dep "org.scalanlp::breeze:2.1.0"

package pso.main

import pso.main.SensorPlacement.*
import pso.core.*
import pso.main.VectorDoubleOps
import scala.util.Random
import scala.io.Source
import java.nio.file.{Files, Path}
import scala.util.Try

import breeze.plot.*
import breeze.linalg.DenseVector
import java.awt.Color

object Main {

  type ScalaVector[T] = scala.collection.immutable.Vector[T]

  /** Genera y muestra una gráfica de dispersión de los sensores sobre el área
    * del campo. Requiere la dependencia de Breeze-Viz.
    * @param coords
    *   Coordenadas finales (X, Y) de los sensores.
    * @param fieldCfg
    *   Configuración del campo para definir los límites.
    */
  def plotSensorPlacement(
      coords: scala.Vector[(Double, Double)],
      fieldCfg: FieldConfig
  ): Unit = {

    // 1. Preparar datos y crear la figura (convertimos a DenseVector sólo para plotting)
    val X = DenseVector(coords.map(_._1).toArray)
    val Y = DenseVector(coords.map(_._2).toArray)
    val f = Figure("Ubicación Óptima de Sensores de Humedad")
    val p = f.subplot(0)

    // 2. Dibujar puntos usando scatter y aprovechando el parámetro `labels` para poner nombres
    // scatter espera funciones indexadas: size: Int => Double, colors: Int => Paint, labels: Int => String
    p += scatter(
      X,
      Y,
      size = (_: Int) => 6.0,
      colors = (_: Int) => Color.RED,
      labels = (i: Int) => s"S${i + 1}"
    )

    // 3. Configurar límites y etiquetas
    p.xlim = (0.0, fieldCfg.width)
    p.ylim = (0.0, fieldCfg.height)
    p.title = s"Colocación Óptima (${fieldCfg.width} x ${fieldCfg.height} m)"
    p.xlabel = "Coordenada X (metros)"
    p.ylabel = "Coordenada Y (metros)"

    f.refresh()
  }

  // Función auxiliar para formatear las coordenadas de salida.
  private def formatCoords(coords: Vector[(Double, Double)]): String = {
    val header =
      "+-------+------------+------------+\n| Sensor|    X (m)   |    Y (m)   |\n+-------+------------+------------+"
    val rows = coords.zipWithIndex
      .map { case ((x, y), i) =>
        f"| S${i + 1}%-5d| ${x}%10.4f | ${y}%10.4f |"
      }
      .mkString("\n")
    s"$header\n$rows\n+-------+------------+------------+"
  }

  /** Punto de entrada principal para la ejecución de la optimización PSO.
    *
    * Configura el campo, carga y alinea los datos de observación, define los
    * límites del problema, inicializa el enjambre y ejecuta el PSO para
    * encontrar la ubicación óptima de los K=10 sensores.
    *
    * @param args
    *   Argumentos de la línea de comandos (no utilizados).
    */
  def main(args: Array[String]): Unit = {
    val width = 500.0; val height = 500.0
    val fieldCfg = FieldConfig(width, height, nx = 30, ny = 30)
    val simCfg = SimulationConfig(field = fieldCfg, nBumps = 0)

    val csvPath = "/home/antonio17/Documents/Scala/PSO/data/cultivos_table.csv"
    if (!Files.exists(Path.of(csvPath))) {
      System.err.println(
        s"[FATAL] Observed CSV requerido pero no encontrado en: $csvPath"
      )
      sys.exit(1)
    }

    val rawObserved = loadObservedCsv(csvPath)
    if (rawObserved.isEmpty) {
      System.err.println(
        "[FATAL] CSV cargado pero sin filas parseables. Abortando."
      )
      sys.exit(1)
    }
    val projected = wgs84ToLocalMeters(rawObserved.toSeq).toVector
    val observedAligned = alignObservedToField(projected, fieldCfg)
    println(
      s"[INFO] Observaciones cargadas y alineadas: ${observedAligned.length} puntos."
    )

    val samplePts = {
      val xs = (0 until fieldCfg.nx).map(i =>
        (i.toDouble / (fieldCfg.nx - 1)) * fieldCfg.width
      )
      val ys = (0 until fieldCfg.ny).map(j =>
        (j.toDouble / (fieldCfg.ny - 1)) * fieldCfg.height
      )
      xs.toVector.flatMap(x => ys.toVector.map(y => (x, y)))
    }

    val avgObs = observedAligned.map(_.humidity).sum / observedAligned.length
    val truthPlaceholder = Vector.fill(samplePts.length)(avgObs)

    // parámetros del problema
    val K = 30
    val dim = 2 * K
    val low = Vector.fill(dim)(0.0)
    val high = Vector.tabulate(dim) { i =>
      if (i % 2 == 0) fieldCfg.width else fieldCfg.height
    }

    val fitnessFn: Vector[Double] => Double =
      makeFitnessWithObs(
        simCfg,
        fieldCfg,
        samplePts,
        truthPlaceholder,
        makeCropWeights(fieldCfg),
        observedAligned
      )

    // PSO config
    val config = PSOConfig[Vector[Double]](
      inertia = 0.7298,
      cognitive = 1.49618,
      social = 1.49618,
      clampVelocity = Some(Vector.fill(dim)(0.1 * math.max(width, height))),
      low = low,
      high = high
    )

    // semilla y Random para inicialización
    val seed = 2025L
    val stdRnd = new Random(seed)

    // helper: generar vector aleatorio entre low/high usando stdRnd
    def randBetweenStd(
        lowV: Vector[Double],
        highV: Vector[Double]
    ): Vector[Double] = {
      lowV.zip(highV).map { case (l, h) => l + stdRnd.nextDouble() * (h - l) }
    }

    val swarmSize = 60
    val iterations = K * 10
    val vMax = 0.1 * math.max(width, height)

    // inicializar partículas determinísticamente con stdRnd
    val particlesInit: Vector[pso.core.Particle[Vector[Double]]] = {
      val builder = Vector.newBuilder[pso.core.Particle[Vector[Double]]]
      var count = 0
      while (count < swarmSize) {
        val pos = randBetweenStd(low, high)
        val velUnit =
          randBetweenStd(Vector.fill(dim)(-1.0), Vector.fill(dim)(1.0))
        val vel = velUnit.map(_ * vMax)
        val fitnessP = fitnessFn(pos)
        builder += pso.core.Particle(pos, vel, pos, fitnessP)
        count += 1
      }
      builder.result()
    }

    val bestP = particlesInit.minBy(_.pBestFitness)

    val initialState =
      pso.core.PSOState(particlesInit, bestP.pBest, bestP.pBestFitness, stdRnd)

    // run PSO
    val (finalState, history) = PSOExtras.runWithHistory(
      initialState,
      config,
      VectorDoubleOps,
      fitnessFn,
      iterations
    )

    println("\n" + "=" * 50)
    println("              RESULTADOS FINALES DE OPTIMIZACIÓN PSO")
    println("=" * 50)

    val bestCoords = vecToCoords(finalState.gBest, fieldCfg)

    val rngLocal = new Random(999L)
    val readingsFinal = sensorReadings(
      bestCoords,
      samplePts,
      truthPlaceholder,
      rngLocal,
      simCfg.sensorNoiseSigma
    )
    val finalObsRmse = observedRMSE(
      bestCoords,
      readingsFinal,
      observedAligned,
      simCfg.idwPower,
      Map("maiz" -> 2.0, "tomate" -> 1.5, "chile" -> 1.2)
    )
    val finalPenalty = minDistancePenalty(bestCoords, simCfg.minDistThresh)

    println(f"Mejor Fitness Global (gBest): ${finalState.gBestFitness}%.6f")
    println(
      f"   (Fitted = RMSE Obs: $finalObsRmse%.6f | Penalización: $finalPenalty%.6f)"
    )
    println(f"   Iteraciones Totales: $iterations")
    println(f"   Umbral de Distancia Mínima: ${simCfg.minDistThresh}%.1f m")

    val n = 30

    println(s"\n--- Historial de Convergencia (Primeros $n) ---")
    history.take(n).zipWithIndex.foreach { case (fit, i) =>
      println(f"Iteración $i: $fit%.9f")
    }

    println(s"\n--- Ubicación Óptima de Sensores (K = $K) ---")
    println(formatCoords(bestCoords))

    println("=" * 50 + "\n")

    println(
      f"Final observed-RMSE = $finalObsRmse%.6f, finalPenalty = $finalPenalty%.6f"
    )

    // 🚀 NUEVO PASO: Generar y mostrar la gráfica de dispersión
    println("[GRÁFICA] Generando visualización de la colocación de sensores...")
    plotSensorPlacement(bestCoords, fieldCfg)

  }

}
