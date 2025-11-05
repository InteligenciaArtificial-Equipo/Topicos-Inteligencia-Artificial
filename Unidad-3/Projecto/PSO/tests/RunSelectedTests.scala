package pso.tests

object RunSelectedTests {
  def main(args: Array[String]): Unit = {
    val suites = Seq(
      "SensorPlacementTests" -> SensorPlacementTest.run(),
      "PSOExtrasSmokeTest" -> PSOExtrasSmokeTest.run(),
      "PSOSmokeStateTest" -> PSOSmokeStateTest.run()
    )

    var totalFails = 0
    for ((name, fails) <- suites) {
      if (fails.isEmpty) println(s"[PASS] $name")
      else {
        println(s"[FAIL] $name -> ${fails.length} failures")
        fails.foreach(f => println(s" - $f"))
        totalFails += fails.length
      }
    }

    if (totalFails == 0) {
      println("ALL SELECTED TESTS PASSED")
      sys.exit(0)
    } else {
      println(s"TOTAL FAILURES: $totalFails")
      sys.exit(1)
    }
  }
}
