package pso.tests

import pso.core.*
import pso.main.VectorDoubleOps
import scala.util.Random

/** Smoke Tests para las estructuras de datos fundamentales del algoritmo PSO
  * (Partícula, Estado y Configuración). Estas pruebas aseguran que las
  * dimensiones y tipos básicos se manejen correctamente en el estado inicial
  * del enjambre y la configuración.
  */
object PSOSmokeStateTest {

  /** Ejecuta las pruebas de inicialización de las estructuras de datos.
    *
    *   1. Verifica la correcta creación y dimensión de [[pso.core.PSOState]].
    *      2. Verifica la correcta creación y asignación de parámetros de
    *      [[pso.core.PSOConfig]].
    *
    * @return
    *   Un [[Vector]] de [[String]] con las descripciones de los fallos.
    */
  def run(): Vector[String] = {
    var fails = Vector.empty[String]

    val dim = 6
    val part = Particle(
      Vector.fill(dim)(0.1),
      Vector.fill(dim)(0.0),
      Vector.fill(dim)(0.1),
      0.123
    )
    val particles = Vector.fill(4)(part)
    val state =
      PSOState(particles, part.pBest, part.pBestFitness, new Random(1L))

    if (state.particles.length != 4)
      fails :+= s"PSOState.particles length wrong: ${state.particles.length}"
    if (state.gBest.length != dim)
      fails :+= s"PSOState.gBest dim mismatch: ${state.gBest.length} expected $dim"

    val cfg = PSOConfig[Vector[Double]](
      inertia = 0.7,
      cognitive = 1.0,
      social = 1.0,
      clampVelocity = Some(Vector.fill(dim)(1.0)),
      low = Vector.fill(dim)(0.0),
      high = Vector.fill(dim)(10.0)
    )
    if (cfg.inertia <= 0.0)
      fails :+= s"PSOConfig.inertia should be positive: ${cfg.inertia}"

    fails
  }

  def main(args: Array[String]): Unit = {
    val fails = run()
    if (fails.isEmpty) println("[PASS] PSOSmokeStateTest")
    else {
      println("[FAIL] PSOSmokeStateTest"); fails.foreach(println); sys.exit(1)
    }
  }
}
