package pso.tests

import pso.core.*
import pso.core.PSOExtras
import pso.main.VectorDoubleOps
import scala.util.Random

/** Smoke Tests para las funciones auxiliares de ejecución definidas en
  * [[pso.core.PSOExtras]].
  *
  * Verifica el comportamiento básico de [[pso.core.PSOExtras.runWithHistory]]
  * cuando se ejecuta con cero iteraciones, asegurando que devuelva el estado
  * inicial inalterado y un historial de longitud 1.
  */
object PSOExtrasSmokeTest {

  /** Ejecuta las pruebas de las funciones auxiliares.
    *
    * @return
    *   Un [[Vector]] de [[String]] con las descripciones de los fallos.
    */
  def run(): Vector[String] = {
    var fails = Vector.empty[String]

    val dim = 4
    val p1 = Particle(
      Vector.fill(dim)(0.0),
      Vector.fill(dim)(0.0),
      Vector.fill(dim)(0.0),
      1.0
    )
    val p2 = Particle(
      Vector.fill(dim)(1.0),
      Vector.fill(dim)(0.0),
      Vector.fill(dim)(1.0),
      0.5
    )
    val p3 = Particle(
      Vector.fill(dim)(2.0),
      Vector.fill(dim)(0.0),
      Vector.fill(dim)(2.0),
      2.0
    )
    val particles = Vector(p1, p2, p3)
    val best = particles.minBy(_.pBestFitness)
    val initialRng = new Random(0L)

    val initialState =
      PSOState(particles, best.pBest, best.pBestFitness, initialRng)

    val config = PSOConfig[Vector[Double]](
      inertia = 0.5,
      cognitive = 1.0,
      social = 1.0,
      clampVelocity = Some(Vector.fill(dim)(1.0)),
      low = Vector.fill(dim)(0.0),
      high = Vector.fill(dim)(10.0)
    )

    val fitnessFn: Vector[Double] => Double = (_: Vector[Double]) =>
      best.pBestFitness

    val (finalState, history) = PSOExtras.runWithHistory(
      initialState,
      config,
      VectorDoubleOps,
      fitnessFn,
      0
    )

    if (finalState.gBestFitness != initialState.gBestFitness)
      fails :+= s"PSOExtras.runWithHistory(0) changed gBestFitness: ${initialState.gBestFitness} -> ${finalState.gBestFitness}"

    if (history.length != 1)
      fails :+= s"PSOExtras.runWithHistory(0) should return history length 1, got ${history.length}"

    if (history.head != initialState.gBestFitness)
      fails :+= s"PSOExtras.runWithHistory(0) history[0] mismatch: got ${history.head} expected ${initialState.gBestFitness}"

    fails
  }

  def main(args: Array[String]): Unit = {
    val fails = run()
    if (fails.isEmpty) println("[PASS] PSOExtrasSmokeTest")
    else {
      println("[FAIL] PSOExtrasSmokeTest"); fails.foreach(println); sys.exit(1)
    }
  }
}
