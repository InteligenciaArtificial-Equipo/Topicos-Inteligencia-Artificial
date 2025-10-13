import scala.compiletime.ops.boolean
import scala.math.abs
import scala.annotation.tailrec
import org.w3c.dom.DOMError

object Recocido {

    /**
     * Runs the Simulated Annealing (SA) algorithm to find an approximate solution 
     * for a combinatorial optimization problem.
     *
     * @tparam A The type representing a solution (e.g., List[Int], Array[Int], etc.)
     *
     * @param initialSolution A function that generates an initial solution of type A.
     * @param nextSolution A function that takes the current solution and generates a neighboring solution.
     * @param startT The starting temperature for the annealing process.
     * @param endT The ending temperature; the algorithm stops when the temperature falls below this value.
     * @param a A cooling function that takes the current temperature and returns the next temperature.
     * @param l A function that returns the number of iterations to perform at a given temperature.
     * @param eval A function that evaluates a solution and returns its "cost" or "score" as an Int. Lower values are considered better.
     *
     * @return The best solution found during the annealing process.
     *
     * The algorithm works as follows:
     * 1. Generate an initial solution.
     * 2. At each temperature, generate a number of candidate neighbor solutions.
     * 3. Accept a neighbor if it improves the solution, or probabilistically accept worse solutions
     *    based on the current temperature (Metropolis criterion).
     * 4. Reduce the temperature according to the cooling function and repeat until the stopping condition.
     */
    def run[A](
        intialSolution: () => A,
        nextSolution: A => A,
        startT: Int,
        endT: Int,
        a: Int => Int,
        l: Int => Int,
        eval: A => Int
    ): A = {

        val solution = intialSolution()

        def random(): Double = {
            util.Random.nextDouble()
        }

        def prob(dif: Int, t: Int): Double = {
            if (t <= 0) 0.0
            else math.exp(-dif.toDouble / t.toDouble)
        }

        @tailrec
        def loop1(
            bestSolution: A,
            currentSolution: A,
            temp: Int,
            i: Int = 0
        ): A = {

            if(temp <= endT || eval(bestSolution) <= 0)
                bestSolution
            else {

                @tailrec
                def loop2(
                    iterations: Int,
                    bestSolution: A,
                    currentSolution: A
                ): (A, A) = {

                    if (iterations <= 0)
                        (bestSolution, currentSolution)
                    else {
                        val neighbor = nextSolution(currentSolution)
                        
                        val evalCurrent = eval(currentSolution)
                        val evallNeigbor = eval(neighbor)

                        println(s"Temperatura: ${temp}")
                        println(s"Iteracion n: ${l(temp) - iterations}")
                        println(s"Solucion Candidata: ${neighbor}")
                        println(s"evaluacion de candidata: ${evallNeigbor}")
                        println()

                        val evalBest = eval(bestSolution)
                        val dif = evallNeigbor - evalCurrent 
                        
                        val best = if (evallNeigbor < evalBest) neighbor else bestSolution
                        if (evallNeigbor == 0)
                            (neighbor, neighbor)
                        else if (dif < 0 || random() < prob(dif = dif, t = temp))
                            loop2(iterations = iterations - 1, bestSolution = best, currentSolution = neighbor)
                        else
                            loop2(iterations = iterations - 1, bestSolution = best, currentSolution = currentSolution)
                    }
                }

                val (best, current) = loop2(iterations = l(temp), bestSolution = bestSolution, currentSolution = currentSolution)            

                loop1(bestSolution = best, currentSolution = current, temp = a(temp), i = i + 1)
            }
            
        }

        loop1(bestSolution = solution, currentSolution = solution, temp = startT)
    }

}

case class Store(need: Int, storage: Int)

case class Center(has: Int, storage: Int)

@main def run(): Unit = {

    val centers: List[Center] = List(
        Center(has = 50, storage = 70),
        Center(has = 45, storage = 60),
        Center(has = 40, storage = 55),
        Center(has = 45, storage = 60)
    )
    val stores: List[Store] = List(
        Store(need = 10, storage = 15),
        Store(need = 12, storage = 20),
        Store(need = 8, storage = 12),
        Store(need = 15, storage = 18),
        Store(need = 10, storage = 15),
        Store(need = 9, storage = 12),
        Store(need = 14, storage = 18),
        Store(need = 11, storage = 15),
        Store(need = 13, storage = 17),
        Store(need = 10, storage = 15),
        Store(need = 12, storage = 20),
        Store(need = 8, storage = 12),
        Store(need = 15, storage = 18),
        Store(need = 10, storage = 15),
        Store(need = 9, storage = 12)
    )

    // ct = centers, n = stores
    val ct = centers.size
    val n = stores.size
    val totalNodes = ct + n

    val adjMat: Array[Array[Double]] = Array.tabulate(totalNodes, totalNodes) { (i, j) =>
        if (i == j) 0.0
        else 1 + util.Random.nextInt(20)
    }

    /**
      * Generate a list with elements bettew 0 - n
      * in random positions.
      * @param n - stores amount
      * @return - initial solution
      */
    def initialSolution(n: Int): List[Int] = {
        util.Random.shuffle((0 until n).toList)
    }

    /**
     * Generates a neighboring solution from the current solution for Simulated Annealing.
     *
     * The neighbor is created by selecting two non-overlapping segments in the list
     * and swapping them. The length of each segment is chosen randomly, subject to
     * the constraint that the segments do not intersect and remain within the list bounds.
     *
     * @param current The current solution represented as a List[Int].
     *                Each element typically corresponds to a node, store, or item in the problem domain.
     * @return A new List[Int] representing a neighboring solution with two segments swapped.
     *
     * Notes:
     * - If the list has fewer than 2 elements, it returns the list unchanged.
     * - Ensures that no IndexOutOfBoundsException occurs by carefully calculating
     *   maximum segment lengths.
     * - Preserves all elements of the original list; only swaps two segments.
     */
    def generateNeigbor(current: List[Int]): List[Int] = {
        val n = current.size
        if (n < 2) return current

        val idx1 = util.Random.nextInt(n)
        var idx2 = util.Random.nextInt(n)
        while (idx2 == idx1) idx2 = util.Random.nextInt(n)

        val (start1, start2) =
            if (idx1 < idx2) (idx1, idx2) else (idx2, idx1)

        val maxLen1 = start2 - start1
        val maxLen2 = n - start2

        val len1 = 1 + util.Random.nextInt(maxLen1)
        val len2 = 1 + util.Random.nextInt(maxLen2)

        val end1 = start1 + len1
        val end2 = start2 + len2

        val segment1 = current.slice(start1, end1)
        val segment2 = current.slice(start2, end2)

        // prefix ++ segment2 ++ middle ++ segment1 ++ suffix
        val prefix = current.take(start1)
        val middle = current.slice(end1, start2)
        val suffix = current.drop(end2)

        prefix ++ segment2 ++ middle ++ segment1 ++ suffix
    }

    /**
     * Evaluates a distribution solution by calculating the total delivery cost.
     *
     * The function simulates delivering products from distribution centers (CDs)
     * to stores in the order specified by `solution`. It follows these rules:
     * 1. A store is served by the current CD if the CD has enough stock.
     * 2. If the CD cannot fully satisfy the store's need, the next CD in the list is used.
     * 3. If no CDs remain, a penalty is applied proportional to the unmet demand.
     * 4. The cost accumulates the distances traveled between nodes using `adjMat`.
     *
     * @param adjMat A 2D array representing distances between nodes.
     *               Indices 0..numCenters-1 correspond to distribution centers,
     *               indices numCenters..numCenters+numStores-1 correspond to stores.
     * @param centers List of distribution centers with current stock and capacity.
     * @param stores List of stores with their demand (`need`) and storage capacity.
     *
     * @param solution A list of store indices representing the order in which stores
     *                 will be visited for delivery.
     *
     * @return An integer representing the total cost of the solution, including
     *         travel distances and penalties for unmet demand. Lower values are better.
     */
    def eval(
        adjMat: Array[Array[Double]], 
        centers: List[Center], 
        stores: List[Store]
        )(solution: List[Int]): Int = {
        
        val numCenters = centers.size

        def process(
            remainingStores: List[Int],
            remainingCenters: List[(Int, Center)],
            currentCD: (Int, Center),
            lastLocation: Int,
            costAcc: Double
        ): Double = remainingStores match {
            case Nil =>
            costAcc + adjMat(lastLocation)(currentCD._1)
            case storeIdx :: tail =>
            val storeNeed = stores(storeIdx).need
            val storeCap = stores(storeIdx).storage

            if (storeNeed <= currentCD._2.has) {
                val distance = adjMat(lastLocation)(storeIdx + numCenters)
                val newCD = currentCD._2.copy(has = currentCD._2.has - storeNeed)
                process(tail, remainingCenters, (currentCD._1, newCD), storeIdx + numCenters, costAcc + distance)
            } else {
                remainingCenters match {
                case Nil =>
                    val penalty = 1000 * (storeNeed - currentCD._2.has).max(0)
                    process(tail, Nil, currentCD, lastLocation, costAcc + penalty)
                case nextCD :: rest =>
                    process(remainingStores, rest, nextCD, lastLocation, costAcc)
                }
            }
        }

        val centersWithIndex = centers.zipWithIndex.map { case (cd, idx) => (idx, cd) }

        centersWithIndex match {
            case Nil => 0
            case firstCD :: rest => process(solution, rest, firstCD, firstCD._1, 0.0).toInt
        }

    }

    val startT = ((n + ct) * .75).toInt
    val endT = 1

    // Iterations per temperature: proportional to the problem size
    val itersPerTemp: Int => Int = t => math.max(10, t / 5)

    // Geometric cooling: T_{k+1} = floor(alpha * T_k)
    val alpha = 0.95
    val coolingGeom: Int => Int = { case t => math.max(1, (t * alpha).toInt) }

    val best = Recocido.run[List[Int]](
        intialSolution = () => initialSolution(n),
        nextSolution = generateNeigbor,
        startT = startT,
        endT = endT,
        a = coolingGeom,
        l = itersPerTemp,
        eval = eval(adjMat, centers, stores)
    )

    /**
     * Distributes stores to distribution centers (CDs) according to the given solution order.
     *
     * This function simulates the allocation of stores to CDs following the same logic as `eval`:
     * - If the current CD has enough stock to fulfill the store's need, the store is assigned to it.
     * - If not, the algorithm moves to the next available CD while keeping the store unassigned.
     * - If no CDs remain, the store is assigned to the last CD (similar to `eval` penalty logic).
     *
     * @param centers List of distribution centers with stock and capacity information.
     * @param stores List of stores with their demand (`need`) and storage capacity.
     * @param solution A list of store indices representing the order in which stores are visited.
     * 
     * @return A list of tuples `(centerIndex, List(storeIndices))` representing the stores assigned
     *         to each CD. The order of the list corresponds to the original order of `centers`.
     */
    def distributeSolution(
        centers: List[Center],
        stores: List[Store]
    )(solution: List[Int]): List[(Int, List[Int])] = {

        val centersWithIndex: List[(Int, Center)] = centers.zipWithIndex.map { case (cd, idx) => (idx, cd) }

        // Inicializamos mapa inmutable con listas vacías por cada centro
        val initAcc: Map[Int, List[Int]] = centersWithIndex.map { case (idx, _) => idx -> List.empty[Int] }.toMap

        @annotation.tailrec
        def loop(
            remainingStores: List[Int],
            remainingCenters: List[(Int, Center)],
            currentCD: (Int, Center),
            acc: Map[Int, List[Int]]
        ): Map[Int, List[Int]] = remainingStores match {
            case Nil =>
            acc
            case storeIdx :: tail =>
            val storeNeed = stores(storeIdx).need

            if (storeNeed <= currentCD._2.has) {
                // CD actual puede surtir: asignar la tienda al CD actual
                val updatedList = acc.updated(currentCD._1, acc.getOrElse(currentCD._1, Nil) :+ storeIdx)
                val newCD = currentCD._2.copy(has = currentCD._2.has - storeNeed)
                loop(tail, remainingCenters, (currentCD._1, newCD), updatedList)
            } else {
                // CD actual no tiene suficiente stock -> intentar siguiente CD
                remainingCenters match {
                case Nil =>
                    // No quedan CDs: asignamos la tienda al CD actual (se comporta como eval con penal)
                    val updatedList = acc.updated(currentCD._1, acc.getOrElse(currentCD._1, Nil) :+ storeIdx)
                    val newCD = currentCD._2.copy(has = 0) // queda sin stock
                    loop(tail, Nil, (currentCD._1, newCD), updatedList)
                case nextCD :: rest =>
                    // Cambiamos de CD y mantenemos la misma tienda por servir
                    loop(remainingStores, rest, nextCD, acc)
                }
            }
        }

        centersWithIndex match {
            case Nil => List.empty
            case firstCD :: rest =>
            val finalMap = loop(solution, rest, firstCD, initAcc)
            // Devolver en orden de índices
            centersWithIndex.map { case (idx, _) => (idx, finalMap.getOrElse(idx, Nil)) }
        }
    }

    println("==================================")
    println(s"Mejor solucion encontrada: $best")
    println()
    println(s"Evaluacion: ${eval(adjMat, centers, stores)(best)}")
    println()
    val distribution = distributeSolution(centers, stores)(best)
    println("Distribución de segmentos por centro (centerIndex -> stores):")
    distribution.foreach { case (cIdx, storeList) =>
        println(s"Centro $cIdx -> tiendas: ${storeList.mkString("[", ", ", "]")}")
    }

}
