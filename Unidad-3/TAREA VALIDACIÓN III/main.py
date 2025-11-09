# -----------------------------------------------------------
# main.py
# Archivo principal que ejecuta el algoritmo genético
# -----------------------------------------------------------

from genetico import poblacion_inicial, clasificar_rutas, nueva_generacion
from visualizacion import graficar_convergencia, graficar_ruta
from datos_prueba import ciudades

def algoritmo_genetico(poblacion, tamano_poblacion, num_elite, tasa_mutacion, generaciones):
    # Crear población inicial
    pop = poblacion_inicial(tamano_poblacion, poblacion)
    progreso = []  # Guardará la mejor distancia de cada generación

    # Distancia inicial
    mejor_inicial = 1 / clasificar_rutas(pop)[0][1]
    print(f"Distancia Inicial: {mejor_inicial:.6f}")

    # Proceso evolutivo
    for g in range(generaciones):
        pop = nueva_generacion(pop, num_elite, tasa_mutacion)
        mejor_distancia = 1 / clasificar_rutas(pop)[0][1]
        progreso.append(mejor_distancia)

    # Distancia final
    mejor_final = 1 / clasificar_rutas(pop)[0][1]
    print(f"\nDistancia Final: {mejor_final:.6f}")

    # Obtener mejor ruta final
    mejor_ruta = pop[clasificar_rutas(pop)[0][0]]

    # Graficar la convergencia
    graficar_convergencia(progreso)
    return mejor_ruta


# -----------------------------------------------------------
# Ejecución principal
# -----------------------------------------------------------
if __name__ == "__main__":
    mejor = algoritmo_genetico(
        poblacion=ciudades,
        tamano_poblacion=80,   # Tamaño de la población
        num_elite=10,          # Número de rutas élite conservadas
        tasa_mutacion=0.01,    # Probabilidad de mutación
        generaciones=500       # Número de generaciones
    )

    print("\nMejor ruta encontrada:")
    print(mejor)

    # Graficar la mejor ruta final
    graficar_ruta(mejor)
