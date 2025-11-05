package pso.core

import scala.util.Random

/** Configuración del algoritmo PSO.
  *
  * @tparam V
  *   tipo que representa vectores (por ejemplo Vector[Double])
  * @param inertia
  *   representa w, el factor de inercia
  * @param cognitive
  *   representa c1, el coeficiente de aceleración cognitiva (atracción hacia el
  *   pBest)
  * @param social
  *   representa c2, el coeficiente de aceleración social (atracción hacia el
  *   gBest)
  * @param clampVelocity
  *   límite absoluto opcional por componente para la velocidad. Si se
  *   proporciona, la velocidad $v$ se limitará a $[-limit, limit]$ por
  *   componente.
  * @param low
  *   lower bounds por componente para la posición.
  * @param high
  *   upper bounds por componente para la posición.
  */
case class PSOConfig[V](
    inertia: Double,
    cognitive: Double,
    social: Double,
    clampVelocity: Option[V],
    low: V,
    high: V
)

/** Funciones utilitarias y motor del PSO.
  *
  * Todas las funciones son puras *en su API* (reciben estado y devuelven nuevo
  * estado). Internamente se usa Random (mutable) que se pasa a través del
  * estado para conveniencia; esto mantiene las llamadas reproducibles cuando se
  * crea el Random con semilla fija en el programa principal.
  */
object PSO {

  /** Ejecuta un solo paso (una iteración) del algoritmo PSO.
    *
    * Actualiza la velocidad y posición de cada partícula, recalcula el `pBest`
    * de cada una y actualiza el `gBest` del enjambre.
    *
    * @tparam V
    *   tipo que representa vectores (por ejemplo Vector[Double]).
    * @param state
    *   estado actual del enjambre (partículas, gBest, rng).
    * @param config
    *   configuración del algoritmo (w, c1, c2, límites).
    * @param ops
    *   implementaciones de operaciones vectoriales para el tipo V.
    * @param problem
    *   función de evaluación (fitness a minimizar).
    * @return
    *   un nuevo [[PSOState]] después de una iteración.
    */
  def step[V](
      state: PSOState[V],
      config: PSOConfig[V],
      ops: VectorOps[V],
      problem: V => Double
  ): PSOState[V] = {

    val zero = ops.zeroLike(config.low)
    val ones = ops.clamp(
      ops.scale(zero, 0.0),
      config.low,
      config.high
    )

    // helper: generar vector aleatorio en [0,1]^d
    def randUnitVector(rng: Random, example: V): V = {
      val low = ops.fill(example, 0.0)
      val high = ops.fill(example, 1.0)
      ops.randBetween(low, high, rng)
    }

    // Procesa una sola partícula y devuelve la partícula nueva.
    // Usamos el Random contenido en state.
    def processParticle(
        particle: Particle[V],
        gBest: V,
        rng: Random
    ): Particle[V] = {
      val r1 = randUnitVector(rng, particle.pos)
      val r2 = randUnitVector(rng, particle.pos)

      // v' = w*v + c1 * r1 * (pBest - pos) + c2 * r2 * (gBest - pos)
      val inertiaPart = ops.scale(particle.vel, config.inertia)
      val cognitivePart = ops.mul(
        r1,
        ops.scale(ops.sub(particle.pBest, particle.pos), config.cognitive)
      )
      val socialPart =
        ops.mul(r2, ops.scale(ops.sub(gBest, particle.pos), config.social))

      val newVelUnclamped =
        ops.add(inertiaPart, ops.add(cognitivePart, socialPart))

      // limitar la velocidad si se proporcionó clampVelocity
      val newVel = config.clampVelocity match {
        case Some(limit) =>
          ops.clamp(newVelUnclamped, ops.scale(limit, -1.0), limit)
        case None => newVelUnclamped
      }

      val newPosPreClamp = ops.add(particle.pos, newVel)
      val newPos = ops.clamp(newPosPreClamp, config.low, config.high)

      val newFitness = problem(newPos)

      val (newPBest, newPBestFitness) =
        if (newFitness < particle.pBestFitness) (newPos, newFitness)
        else (particle.pBest, particle.pBestFitness)

      Particle(newPos, newVel, newPBest, newPBestFitness)
    }

    val rng = state.rng
    // procesar todas las partículas
    val newParticles =
      state.particles.map(p => processParticle(p, state.gBest, rng))

    // actualizar gBest
    val bestParticle = newParticles.minBy(_.pBestFitness)
    val (nextGBest, nextGBestFitness) =
      if (bestParticle.pBestFitness < state.gBestFitness)
        (bestParticle.pBest, bestParticle.pBestFitness)
      else (state.gBest, state.gBestFitness)

    PSOState(newParticles, nextGBest, nextGBestFitness, rng)
  }

  /** Ejecuta el algoritmo PSO durante un número específico de iteraciones.
    *
    * Aplica la función [[step]] de forma iterativa usando `foldLeft`.
    *
    * @tparam V
    *   tipo que representa vectores.
    * @param initial
    *   estado inicial del enjambre.
    * @param config
    *   configuración del algoritmo.
    * @param ops
    *   implementaciones de operaciones vectoriales.
    * @param problem
    *   función de evaluación (fitness a minimizar).
    * @param iterations
    *   número total de iteraciones a ejecutar.
    * @return
    *   el [[PSOState]] final después de todas las iteraciones.
    */
  def run[V](
      initial: PSOState[V],
      config: PSOConfig[V],
      ops: VectorOps[V],
      problem: V => Double,
      iterations: Int
  ): PSOState[V] =
    (0 until iterations).foldLeft(initial) { (s, _) =>
      step(s, config, ops, problem)
    }

  /** Inicializa un nuevo enjambre de partículas.
    *
    * Utiliza una función `initPosVel` para determinar la posición y velocidad
    * iniciales de cada partícula y calcula el `gBest` inicial.
    *
    * @tparam V
    *   tipo que representa vectores.
    * @param rng
    *   generador de números aleatorios a usar.
    * @param swarmSize
    *   el número de partículas en el enjambre.
    * @param initPosVel
    *   función que, dado un [[scala.util.Random]], devuelve un par `(posición,
    *   velocidad)` inicial.
    * @param problem
    *   función de evaluación (fitness a minimizar).
    * @return
    *   el [[PSOState]] inicial del enjambre.
    */
  def initialize[V](
      rng: Random,
      swarmSize: Int,
      initPosVel: Random => (V, V),
      problem: V => Double
  ): PSOState[V] = {

    def loop(i: Int, acc: Vector[Particle[V]]): Vector[Particle[V]] =
      if (i <= 0) acc
      else {
        val (pos, vel) = initPosVel(rng)
        val fitness = problem(pos)
        val p = Particle(pos, vel, pos, fitness)
        loop(i - 1, acc :+ p)
      }

    val particles = loop(swarmSize, Vector.empty)
    val best = particles.minBy(_.pBestFitness)
    PSOState(particles, best.pBest, best.pBestFitness, rng)
  }
}
