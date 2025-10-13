# Proyecto: Recocido Simulado para Enrutamiento Logístico (Scala 3)

## Integrantes
- Peñuelas López Luis Antonio
- Peraza Medina Eliezer Daniel

**Descripción**
Este proyecto implementa una versión de *Simulated Annealing* (Recocido Simulado) para optimizar rutas de distribución de productos desde centros de distribución (CD) hacia tiendas/sucursales. Está orientado a un escenario con **múltiples centros** y una lista ordenada de tiendas. La evaluación asigna secuencias de tiendas a CDs siguiendo una política secuencial (DP-like): el CD actual sirve tiendas mientras tenga stock; si no puede, se pasa al siguiente CD.

**Lenguaje**
- Scala 3 (probado con Scala 3.3.1). Ejecutar con `scala-cli` o sbt si adaptas el proyecto.

---

## Estructura del código principal (`Proyecto.scala`)
- `object Recocido`
  - `def run[A](intialSolution, nextSolution, startT, endT, a, l, eval): A`
    - Implementación genérica del algoritmo de Recocido Simulado.
    - Parámetros: función generadora de solución inicial, generador de vecinos, temperatura inicial/final, función de enfriamiento `a`, iteraciones por temperatura `l`, y función de evaluación `eval`.
- `case class Store(need: Int, storage: Int)`
  - Representa una tienda con demanda y capacidad de almacenamiento.
- `case class Center(has: Int, storage: Int)`
  - Representa un centro de distribución con stock disponible y capacidad.
- `initialSolution(n: Int): List[Int]`
  - Genera una solución inicial aleatoria (lista de índices de tiendas).
- `generateNeighbor(current: List[Int]): List[Int]`
  - Genera un vecino intercambiando dos segmentos no solapados (longitudes aleatorias).
- `eval(adjMat, centers, stores)(solution: List[Int]): Int`
  - Evalúa la solución calculando coste total (distancias + penalizaciones por falta de stock).
  - Asume que la matriz de adyacencia tiene primero los `ct` centros y luego las `n` tiendas.
- `distributeSolution(centers, stores)(solution: List[Int]): List[(Int, List[Int])]`
  - Devuelve cómo quedaron asignadas las tiendas a cada centro (útil para visualizar el resultado).

---

## Cómo ejecutar (ejemplo con `scala-cli`)
1. Asegúrate de tener instalado `scala-cli` o un entorno para Scala 3.
2. En el directorio con `Proyecto.scala`, ejecuta:
   ```bash
   scala-cli run Project.scala
   ```
   Esto compilará y ejecutará el programa. El `main` contiene un ejemplo con 4 centros y 15 tiendas y una matriz de distancias generada aleatoriamente.

---

## Configuración rápida
- **Centros**: en `main` modifica la lista `centers: List[Center]` para ajustar `has` (stock inicial) y `storage`.
- **Tiendas**: en `main` modifica la lista `stores: List[Store]` para ajustar `need` y `storage`.
- **Matriz de adyacencia (`adjMat`)**: actualmente se genera aleatoria. Se puede reemplazar por distancias reales.
  - Importante: la matriz debe tener tamaño `ct + n` donde los índices `0..ct-1` son centros y `ct..ct+n-1` son tiendas.
- **Parámetros del Recocido**
  - `startT`: temperatura inicial — puede basarse en el tamaño del problema.
  - `endT`: temperatura final (parada).
  - `itersPerTemp`: función `Int => Int` para iteraciones por temperatura.
  - `coolingGeom`: factor geométrico `alpha` para enfriamiento (`T_{k+1} = floor(alpha * T_k)`).

---

## Salida e interpretación
- El programa imprime iteraciones y candidatos durante la ejecución y finalmente:
  - `Best solution found: ...` — lista de índices de tiendas ordenada (solución).
  - `Score: ...` — coste total calculado por `eval`.
- Usar `distributeSolution` con la solución `best` para ver la asignación por centro:
  - Muestra `Centro i -> [lista de tiendas]` según índices relativos a la lista `stores` (0..n-1).
  - Si deseas índices absolutos en la matriz `adjMat`, suma `ct` al índice de la tienda.
