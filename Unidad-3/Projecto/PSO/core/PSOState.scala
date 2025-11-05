package pso.core

import scala.util.Random

/** Estado inmutable del enjambre de partículas en una iteración específica del
  * algoritmo PSO.
  *
  * Esta clase encapsula toda la información necesaria para ejecutar el
  * siguiente paso del algoritmo de forma pura y reproducible.
  *
  * @tparam V
  *   tipo que representa vectores (por ejemplo Vector[Double])
  * @param particles
  *   Un vector que contiene todas las [[Particle]] que forman el enjambre.
  * @param gBest
  *   La mejor posición encontrada hasta el momento por **cualquier** partícula
  *   en el enjambre (Global Best).
  * @param gBestFitness
  *   El valor de la función objetivo (fitness) asociado a la posición $gBest$.
  *   Es el valor mínimo encontrado.
  * @param rng
  *   El generador de números aleatorios (`scala.util.Random`) que se utiliza
  *   para mantener la reproducibilidad de las iteraciones.
  */
case class PSOState[V](
    particles: Vector[Particle[V]],
    gBest: V,
    gBestFitness: Double,
    rng: Random
)
