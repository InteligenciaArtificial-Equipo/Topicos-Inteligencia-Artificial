# Optimización de Colocación de Sensores de Humedad (PSO)

## Integrantes
- Peñuelas López Luis Antonio
- Peraza Medina Eliezer Daniel

## Resumen del Proyecto

Este proyecto implementa el **Algoritmo de Optimización por Enjambre de Partículas (PSO)** para determinar la ubicación óptima de $K$ sensores de humedad en un campo agrícola de $500 \times 500$ metros.

El objetivo principal es **minimizar el error de estimación** de la humedad del suelo, utilizando la **Interpolación por Distancias Inversas (IDW)** para predecir los valores de humedad. El modelo utiliza una función de fitness combinada que equilibra el error sobre una malla sintética y la fidelidad a los datos de observación reales, mientras penaliza la colocación de sensores demasiado cercanos.

**Tecnología:** Scala 3 y Scala-CLI.

## Metodología de Optimización

### Función Objetivo (Fitness)

La función de fitness $F(\mathbf{x})$ es no-diferenciable, no-convexa, y está diseñada para equilibrar múltiples factores.

$$F(\mathbf{x}) = w_{obs} \cdot \text{RMSE}_{obs}(\mathbf{x}) + w_{grid} \cdot \text{RMSE}_{grid}(\mathbf{x}) + \alpha_{P} \cdot P(\mathbf{x})$$

Donde:
* $\text{RMSE}_{obs}$: Error sobre los puntos de observación reales.
* $\text{RMSE}_{grid}$: Error sobre la malla sintética de $30 \times 30$ puntos (900 puntos).
* $P(\mathbf{x})$: Penalización cuadrática por violar la distancia mínima ($10.0$ metros).

### Representación de Soluciones

* **Dimensión del Problema:** $2K$.
* **Vector de Posición ($\mathbf{x}$):** Un vector de 60 componentes (para $K=30$ sensores): $\mathbf{x} = (x_1, y_1, x_2, y_2, \dots, x_{30}, y_{30})$.

## Resultados Clave (Corrida con K=30)

La ejecución del PSO demostró una convergencia muy rápida y una alta precisión.

| Métrica | Valor | Observación |
| :--- | :--- | :--- |
| **Mejor Fitness Global** | **0.109481** | Métrica combinada de error. |
| **RMSE en Observaciones** | **0.112560** | Alta fidelidad a los datos empíricos. |
| **Penalización** | 0.000000 | No se violaron las restricciones de distancia. |
| **Número de Sensores (K)** | 30 | Aumento de densidad de muestreo. |
| **Iteraciones** | K * 10 | (El óptimo se alcanzó en las primeras 8 iteraciones). Con los datos del CSV |

**Conclusión Principal:** El aumento a 30 sensores mejoró sustancialmente la precisión predictiva. El algoritmo fue eficiente en encontrar el óptimo, aunque las K * 10 iteraciones fueron excesivas debido a la rápida convergencia.

## Estructura del Proyecto

El código está organizado modularmente para separar la lógica de optimización del problema específico:

* `pso.core`: Contiene las clases y *traits* genéricos del algoritmo PSO (`Particle`, `PSOState`, `VectorOps`).
* `pso.main`: Contiene la lógica de la aplicación (`SensorPlacement.scala`), incluyendo el simulador, el cálculo de IDW, la función de fitness y el método `main`.
* `data/`: Archivos de datos de entrada (`cultivos_table.csv`).

## Ejecución

### Requisitos

* **Scala-CLI:** Utilizado para manejar dependencias y compilación.
* **Java (JVM 21 o superior):** Necesario para ejecutar Scala.

#### Para ejecucion:

> scala-cli run . --main-class pso.main.Main

#### Para Tests

> scala-cli run . --main-class pso.tests.RunSelectedTests
