# -----------------------------------------------------------
# visualizacion.py
# Contiene las funciones para graficar los resultados del algoritmo:
# - graficar_convergencia: muestra cómo mejora la distancia
# - graficar_ruta: muestra la mejor ruta final
# -----------------------------------------------------------

import matplotlib.pyplot as plt

def graficar_convergencia(progreso):
    # Muestra cómo evoluciona la mejor distancia en cada generación
    plt.figure(figsize=(8, 4))
    plt.plot(progreso, marker='o')
    plt.title("Evolución del Algoritmo Genético")
    plt.xlabel("Generación")
    plt.ylabel("Distancia de la mejor ruta")
    plt.grid(True)
    plt.show()

def graficar_ruta(ruta):
    # Dibuja la mejor ruta final
    x = [c.x for c in ruta] + [ruta[0].x]
    y = [c.y for c in ruta] + [ruta[0].y]

    plt.figure(figsize=(7, 6))
    plt.plot(x, y, marker='o', linestyle='-')
    for c in ruta:
        plt.text(c.x, c.y, f"{c.nombre}", fontsize=9, ha='right')
    plt.title("Mejor Ruta Encontrada")
    plt.xlabel("Coordenada X")
    plt.ylabel("Coordenada Y")
    plt.grid(True)
    plt.show()
