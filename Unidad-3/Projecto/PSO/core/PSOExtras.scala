package pso.core

/** PSOExtras: utilidades puras para iterar, detener y registrar la ejecución
  * del PSO. Este objeto complementa la implementación central (PSO.scala) sin
  * efectos colaterales, basándose en la inmutabilidad de [[PSOState]]. Permite
  * inspeccionar estados intermedios, aplicar condiciones de parada
  * personalizadas y obtener el historial de convergencia.
  */
object PSOExtras {

  /** Genera un [[LazyList]]de estados del PSO.
    *
    * El primer elemento de la lista es el estado `initial`, el siguiente es el
    * resultado de `PSO.step(initial)`, y así sucesivamente de forma infinita.
    *
    * @tparam V
    *   tipo que representa vectores.
    * @param initial
    *   El estado inicial del enjambre.
    * @param config
    *   La configuración del algoritmo PSO.
    * @param ops
    *   Implementaciones de operaciones vectoriales.
    * @param problem
    *   Función de evaluación (fitness a minimizar).
    * @return
    *   Un `LazyList` infinito de [[PSOState]] generados secuencialmente.
    */
  def states[V](
      initial: PSOState[V],
      config: PSOConfig[V],
      ops: VectorOps[V],
      problem: V => Double
  ): LazyList[PSOState[V]] =
    LazyList.iterate(initial)(s => PSO.step(s, config, ops, problem))

  /** Ejecuta el algoritmo PSO hasta que se cumpla una condición de parada o se
    * alcance un número máximo de iteraciones.
    *
    * Detiene la ejecución en la primera iteración (contando desde 0) para la
    * cual la función `stop` devuelve `true`, o después de `maxIter` pasos si no
    * se cumple.
    *
    * @tparam V
    *   tipo que representa vectores.
    * @param initial
    *   El estado inicial del enjambre (iteración 0).
    * @param config
    *   La configuración del algoritmo PSO.
    * @param ops
    *   Implementaciones de operaciones vectoriales.
    * @param problem
    *   Función de evaluación (fitness a minimizar).
    * @param stop
    *   Función que toma un [[PSOState]] y devuelve `true` si se debe detener la
    *   ejecución.
    * @param maxIter
    *   Número máximo de pasos de $PSO.step$ a ejecutar (además de la iteración
    *   0).
    * @return
    *   El [[PSOState]] en el que se detuvo la ejecución (el que cumple `stop`,
    *   o el último si se alcanzó `maxIter`).
    */
  def runUntil[V](
      initial: PSOState[V],
      config: PSOConfig[V],
      ops: VectorOps[V],
      problem: V => Double,
      stop: PSOState[V] => Boolean,
      maxIter: Int
  ): PSOState[V] = {
    val seq = states(initial, config, ops, problem).zipWithIndex
      .take(maxIter + 1)
      .toList
    seq.find { case (s, _) => stop(s) } match {
      case Some((s, _)) => s
      case None         => seq.last._1
    }
  }

  /** Ejecuta el algoritmo PSO durante un número fijo de iteraciones y registra
    * el historial de convergencia.
    *
    * Devuelve el estado final del enjambre junto con un historial de la aptitud
    * del mejor global (`gBestFitness`) para cada paso, incluyendo el estado
    * inicial (iteración 0).
    *
    * @tparam V
    *   tipo que representa vectores.
    * @param initial
    *   El estado inicial del enjambre (iteración 0).
    * @param config
    *   La configuración del algoritmo PSO.
    * @param ops
    *   Implementaciones de operaciones vectoriales.
    * @param problem
    *   Función de evaluación (fitness a minimizar).
    * @param iterations
    *   Número de pasos de $PSO.step$ a ejecutar.
    * @return
    *   Una tupla que contiene:
    *   1. El [[PSOState]] final después de `iterations` pasos. 2. Un [[Vector]]
    *      de [[scala.Double]] con el historial de `gBestFitness` para las
    *      `iterations + 1` iteraciones.
    */
  def runWithHistory[V](
      initial: PSOState[V],
      config: PSOConfig[V],
      ops: VectorOps[V],
      problem: V => Double,
      iterations: Int
  ): (PSOState[V], Vector[Double]) = {
    val seq = states(initial, config, ops, problem).zipWithIndex
      .take(iterations + 1)
      .toList
    val history = seq.map { case (s, _) => s.gBestFitness }.toVector
    (seq.last._1, history)
  }
}
