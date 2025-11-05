package pso.main

import pso.core.VectorOps
import scala.util.Random

/** Implementación concreta del trait [[pso.core.VectorOps]] utilizando
  * [[Vector]] de [[Double]].
  *
  * Este objeto define todas las operaciones aritméticas y de generación de
  * números aleatorios necesarias para que el algoritmo PSO manipule las
  * posiciones y velocidades de las partículas, donde cada posición es un vector
  * de coordenadas.
  *
  * En el contexto del problema de los sensores, un vector V de tamaño K * 2 (o
  * simplemente 2K) representa las coordenadas (x, y) de los K sensores a
  * optimizar.
  */
object VectorDoubleOps extends VectorOps[Vector[Double]] {

  /** Suma componente a componente de dos vectores de dobles.
    * @param a
    *   Primer vector.
    * @param b
    *   Segundo vector.
    * @return
    *   El resultado de la suma elemento a elemento: a(i) + b(i).
    */
  override def add(a: Vector[Double], b: Vector[Double]): Vector[Double] =
    a.zip(b).map { case (x, y) => x + y }

  /** Resta componente a componente de dos vectores de dobles.
    * @param a
    *   Vector minuendo.
    * @param b
    *   Vector sustraendo.
    * @return
    *   El resultado de la resta elemento a elemento: a(i) - b(i).
    */
  override def sub(a: Vector[Double], b: Vector[Double]): Vector[Double] =
    a.zip(b).map { case (x, y) => x - y }

  /** Multiplica cada componente del vector por un escalar.
    * @param v
    *   Vector a escalar.
    * @param scalar
    *   Factor de escala.
    * @return
    *   El resultado de la multiplicación: v(i) * scalar.
    */
  override def scale(v: Vector[Double], scalar: Double): Vector[Double] =
    v.map(_ * scalar)

  /** Realiza la multiplicación componente a componente (Producto de Hadamard).
    * Este se usa típicamente para aplicar los factores de aleatoriedad r1 y r2
    * a las partes cognitiva y social de la fórmula de velocidad del PSO.
    * @param a
    *   Primer vector.
    * @param b
    *   Segundo vector.
    * @return
    *   El resultado de la multiplicación elemento a elemento: a(i) * b(i).
    */
  override def mul(a: Vector[Double], b: Vector[Double]): Vector[Double] =
    a.zip(b).map { case (x, y) => x * y }

  /** Crea un nuevo vector de ceros con la misma dimensión que el vector de
    * ejemplo.
    * @param example
    *   Vector de referencia.
    * @return
    *   Un vector de [[Double]] lleno de 0.0.
    */
  override def zeroLike(example: Vector[Double]): Vector[Double] =
    Vector.fill(example.length)(0.0)

  /** Restringe los valores de un vector dentro de los límites bajos y altos por
    * componente. Se utiliza para limitar la posición (dentro del campo) y la
    * velocidad (si aplica `clampVelocity`).
    * @param v
    *   Vector de valores a restringir.
    * @param low
    *   Vector de límites inferiores.
    * @param high
    *   Vector de límites superiores.
    * @return
    *   Un nuevo vector donde cada componente v(i) está dentro del rango
    *   [low(i), high(i)].
    */
  override def clamp(
      v: Vector[Double],
      low: Vector[Double],
      high: Vector[Double]
  ): Vector[Double] =
    v.zip(low.zip(high)).map { case (x, (l, h)) =>
      if (x < l) l else if (x > h) h else x
    }

  /** Genera un vector aleatorio donde cada componente está uniformemente
    * distribuido entre sus límites correspondientes [low(i), high(i)].
    * @param low
    *   Vector de límites inferiores.
    * @param high
    *   Vector de límites superiores.
    * @param rng
    *   Generador de números aleatorios.
    * @return
    *   Un nuevo vector de [[Double]] aleatorio.
    */
  override def randBetween(
      low: Vector[Double],
      high: Vector[Double],
      rng: Random
  ): Vector[Double] = {
    def loop(i: Int, r: Random, acc: Vector[Double]): Vector[Double] =
      if (i >= low.length) acc
      else {
        val d = r.nextDouble
        val value = low(i) + d * (high(i) - low(i))
        loop(i + 1, r, acc :+ value)
      }

    loop(0, rng, Vector.empty)
  }

  /** Devuelve la dimensión (longitud) del vector.
    * @param v
    *   Vector de entrada.
    * @return
    *   La longitud del vector.
    */
  override def dimension(v: Vector[Double]): Int = v.length

  /** Crea un nuevo vector relleno con el valor escalar dado.
    * @param example
    *   Vector de referencia para determinar la longitud.
    * @param value
    *   El valor a usar para rellenar el nuevo vector.
    * @return
    *   Un vector de [[Double]] relleno con `value`.
    */
  override def fill(example: Vector[Double], value: Double): Vector[Double] =
    Vector.fill(example.length)(value)
}
