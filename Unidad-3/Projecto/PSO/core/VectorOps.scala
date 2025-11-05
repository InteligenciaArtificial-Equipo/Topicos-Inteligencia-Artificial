package pso.core

import scala.util.Random

/** Interfaz genérica para operaciones vectoriales que requiere el algoritmo
  * PSO.
  *
  * Este trait abstrae las operaciones aritméticas y de generación aleatoria
  * sobre los vectores del PSO, permitiendo que el algoritmo sea completamente
  * genérico respecto a la estructura de datos utilizada (ej. `Vector[Double]`,
  * `Array[Double]`, etc.).
  *
  * @tparam V
  *   Tipo de dato que representa la posición o velocidad de una partícula. Por
  *   ejemplo, `Vector[Double]` o `Array[Double]`.
  */
trait VectorOps[V] {

  /** * Realiza la suma elemento a elemento de dos vectores del mismo tamaño. *
    * \@param a Primer vector.
    * @param b
    *   Segundo vector.
    * @return
    *   Un nuevo vector resultante de la suma a + b.
    */
  def add(a: V, b: V): V

  /** Realiza la resta elemento a elemento de dos vectores del mismo tamaño.
    * @param a
    *   Vector minuendo.
    * @param b
    *   Vector sustraendo.
    * @return
    *   Un nuevo vector resultante de la resta a - b.
    */
  def sub(a: V, b: V): V

  /** Escala todos los elementos de un vector por un factor escalar.
    * @param v
    *   Vector a escalar.
    * @param scalar
    *   Factor de escala.
    * @return
    *   Un nuevo vector resultante de la multiplicación v * scalar.
    */
  def scale(v: V, scalar: Double): V

  /** Realiza la multiplicación elemento a elemento (Producto de Hadamard).
    * @param a
    *   Primer vector.
    * @param b
    *   Segundo vector.
    * @return
    *   Un nuevo vector resultante de multiplicar componente por componente los
    *   vectores a y b.
    */
  def mul(a: V, b: V): V

  /** Devuelve un vector de ceros con la misma forma y dimensión que el vector
    * de ejemplo. Se usa para inicializar estructuras intermedias o vectores
    * nulos.
    * @param example
    *   Vector de referencia para la forma y dimensión.
    * @return
    *   Un nuevo vector donde todos los componentes son 0.0.
    */
  def zeroLike(example: V): V

  /** Restringe o 'satura' los valores del vector `v` dentro de los límites
    * `[low, high]` de cada componente, asegurando que el valor resultante en la
    * posición `i` se encuentre entre `low(i)` y `high(i)`. * @param v Vector a
    * restringir.
    * @param low
    *   Vector de límites inferiores por componente.
    * @param high
    *   Vector de límites superiores por componente.
    * @return
    *   Un nuevo vector con todos los valores restringidos a los límites.
    */
  def clamp(v: V, low: V, high: V): V

  /** Genera un nuevo vector aleatorio cuyos componentes individuales `i` están
    * en el rango [low(i), high(i)].
    *
    * @param low
    *   Vector de límites inferiores.
    * @param high
    *   Vector de límites superiores.
    * @param rng
    *   Generador de números aleatorios (`scala.util.Random` mutable).
    * @return
    *   Un nuevo vector V con valores dentro de los límites dados.
    */
  def randBetween(low: V, high: V, rng: Random): V

  /** Devuelve la dimensión (número de componentes) del vector.
    * @param v
    *   Vector a medir.
    * @return
    *   La dimensión del espacio vectorial.
    */
  def dimension(v: V): Int

  /** Crea un vector con el mismo tamaño y forma que `example`, rellenado con el
    * valor escalar `value` en todos sus componentes.
    * @param example
    *   Vector de referencia para la forma y dimensión.
    * @param value
    *   Valor con el que se rellenarán todos los componentes.
    * @return
    *   Un nuevo vector donde todos los componentes son `value`.
    */
  def fill(example: V, value: Double): V
}
