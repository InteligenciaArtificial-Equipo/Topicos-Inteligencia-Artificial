package pso.core

/** Representación inmutable de una partícula dentro del enjambre PSO.
  *
  * Almacena el estado completo de una partícula en un momento dado.
  *
  * @tparam V
  *   tipo que representa vectores (por ejemplo Vector[Double])
  * @param pos
  *   la posición actual de la partícula en el espacio de búsqueda.
  * @param vel
  *   la velocidad actual de la partícula.
  * @param pBest
  *   la mejor posición encontrada históricamente por esta partícula (personal
  *   best).
  * @param pBestFitness
  *   el valor de la función objetivo (fitness) asociado a la mejor posición
  *   histórica.
  */
case class Particle[V](pos: V, vel: V, pBest: V, pBestFitness: Double)
